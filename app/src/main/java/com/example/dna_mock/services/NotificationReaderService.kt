package com.example.dna_mock.services

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationManagerCompat
import com.example.dna_mock.ChatResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale

/** Differentiated risk levels for incoming notifications. */
enum class NotificationRisk {
    HIGH_RISK,
    SUSPICIOUS,
    VERIFY,
    SAFE,
    UNKNOWN
}

/** Temporary on-device previews only; nothing is written to disk. */
data class NotificationPreview(
    val key: String,
    val appName: String,
    val title: String,
    val content: String,
    val postedAt: Long,
    val risk: NotificationRisk
)

/** Observable list for the UI. Must only be modified on the main thread. */
object NotificationInbox {
    val items = mutableStateListOf<NotificationPreview>()

    fun replace(next: List<NotificationPreview>) {
        items.clear()
        items.addAll(next)
    }

    fun suspiciousNotifications(): List<NotificationPreview> =
        items.filter {
            it.risk == NotificationRisk.HIGH_RISK ||
                    it.risk == NotificationRisk.SUSPICIOUS ||
                    it.risk == NotificationRisk.VERIFY
        }

    fun updateRisk(key: String, risk: NotificationRisk) {
        val index = items.indexOfFirst { it.key == key }
        if (index != -1) {
            items[index] = items[index].copy(risk = risk)
        }
    }
}

fun isNotificationReaderEnabled(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/**
 * Android only connects this service after the user opts in through system Settings.
 *
 * PRIVACY: the title and text of each notification are sent to the assistant backend for analysis
 * (the ChatService URL). Nothing is stored on the device, and the server session is deleted right
 * after each analysis. Tell users about this, and ideally ask for a separate in-app opt-in before
 * enabling the analysis, because notifications can contain codes and private messages.
 */
class NotificationReaderService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val chatService = ChatService() // body logging stays OFF: notifications are private
    private val analysisLimiter = Semaphore(MAX_PARALLEL_ANALYSES)

    /** Result of a past analysis, so each notification is analysed only ONCE, not on every refresh. */
    private data class CachedRisk(val risk: NotificationRisk, val fromFallback: Boolean, val at: Long)

    private val riskCache = object : LinkedHashMap<String, CachedRisk>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedRisk>?) = size > MAX_CACHE
    }
    private val inFlight = HashSet<String>()

    companion object {
        private const val MAX_PARALLEL_ANALYSES = 3
        private const val MAX_CACHE = 200
        private const val FALLBACK_RETRY_MS = 60_000L // don't hammer an unreachable server

        private var connected: NotificationReaderService? = null
        fun refreshIfConnected() {
            connected?.refreshActiveNotifications()
        }

        private val URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    }

    // ---- listener callbacks ----------------------------------------------------------------

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = this
        refreshActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        if (connected === this) connected = null
        NotificationInbox.replace(emptyList())
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        if (connected === this) connected = null
        serviceScope.cancel() // the old code leaked this scope and kept running network calls
        mainHandler.removeCallbacksAndMessages(null)
        NotificationInbox.replace(emptyList())
        super.onDestroy()
    }

    // ---- refresh ---------------------------------------------------------------------------

    /**
     * Publishes the current notifications IMMEDIATELY (with the cached risk, or UNKNOWN), then
     * analyses only the ones that have no result yet. Each result updates its own row as it arrives.
     * (Before: every post/removal re-sent all 30 notifications one by one and the list stayed
     * empty until all calls finished; overlapping refreshes could overwrite newer data.)
     */
    private fun refreshActiveNotifications() {
        val active = runCatching { activeNotifications?.toList().orEmpty() }
            .getOrDefault(emptyList())

        val previews = active.asSequence()
            .filter { it.packageName != packageName }                                   // not our own notifications
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .filter { (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 } // music players, timers ...
            .mapNotNull { it.asPreview() }
            .sortedByDescending { it.postedAt }
            .take(30)
            .toList()

        val now = System.currentTimeMillis()
        val cached = previews.associateWith { cachedRisk(it, now) }
        val shown = previews.map { it.copy(risk = cached[it] ?: NotificationRisk.UNKNOWN) }
        mainHandler.post { NotificationInbox.replace(shown) }

        previews.filter { cached[it] == null }.forEach { startAnalysis(it) }
    }

    private fun startAnalysis(preview: NotificationPreview) {
        val id = cacheKey(preview)
        synchronized(inFlight) { if (!inFlight.add(id)) return } // already being analysed

        serviceScope.launch {
            try {
                val (risk, fromFallback) = analysisLimiter.withPermit { analyse(preview) }
                synchronized(riskCache) {
                    riskCache[id] = CachedRisk(risk, fromFallback, System.currentTimeMillis())
                }
                mainHandler.post {
                    // apply only if the row still shows the SAME content (it may have been updated or removed)
                    val current = NotificationInbox.items.firstOrNull { it.key == preview.key }
                    if (current != null && cacheKey(current) == id) {
                        NotificationInbox.updateRisk(preview.key, risk)
                    }
                }
            } finally {
                synchronized(inFlight) { inFlight.remove(id) }
            }
        }
    }

    private fun cachedRisk(p: NotificationPreview, now: Long): NotificationRisk? {
        val hit = synchronized(riskCache) { riskCache[cacheKey(p)] } ?: return null
        return if (hit.fromFallback && now - hit.at > FALLBACK_RETRY_MS) null else hit.risk
    }

    /** Same notification key + same text = same analysis. An edited notification is analysed again. */
    private fun cacheKey(p: NotificationPreview) = "${p.key}#${(p.title + "\u0000" + p.content).hashCode()}"

    private fun StatusBarNotification.asPreview(): NotificationPreview? {
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val content = (
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                )?.toString()?.trim().orEmpty()
        if (title.isBlank() && content.isBlank()) return null
        val readableAppName = runCatching {
            val app = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(app).toString()
        }.getOrDefault(packageName)

        return NotificationPreview(
            key = key,
            appName = readableAppName,
            title = title.ifBlank { readableAppName }.take(120),
            content = content.take(700),
            postedAt = postTime,
            risk = NotificationRisk.UNKNOWN
        )
    }

    // ---- analysis --------------------------------------------------------------------------

    /**
     * Sends ONE notification to the backend and maps the answer to a [NotificationRisk].
     * Returns the risk and whether it came from the offline fallback.
     *
     * The notification is sent the way the backend expects an inspected message: its text as the
     * message plus an `artifact` signal (type "push_notification"). The old wrapper prompt
     * ("Analyze notification ... Determine risk.") skewed the model: a genuine bank login-code
     * notice came back "high" with it and "none" without it.
     */
    private suspend fun analyse(preview: NotificationPreview): Pair<NotificationRisk, Boolean> {
        val text = "${preview.title}: ${preview.content}".trim().take(4000)
        val links = URL_REGEX.findAll(preview.content)
            .map { it.value.trimEnd('.', ',', ')', ';') }
            .distinct().take(5).toList()

        val signals = JsonObject().apply {
            add("artifact", JsonObject().apply {
                addProperty("type", "push_notification")
                addProperty("sender", preview.appName)
                addProperty("body", text)
                add("links", JsonArray().also { arr -> links.forEach { arr.add(it) } })
            })
        }

        // sessionId = null -> every notification gets its own isolated server session
        val result = chatService.sendMessage(
            message = text,
            sessionId = null,
            audience = "consumer",
            signals = signals
        )

        return result.fold(
            onSuccess = { chatResult ->
                // free the server-side session (it would otherwise live for an hour per notification)
                serviceScope.launch { chatService.resetSession(chatResult.response.session_id) }
                mapRisk(chatResult, preview) to false
            },
            onFailure = { unreachableRisk(preview) to true }
        )
    }

    /**
     * What gets flagged = the MORE SEVERE of two independent opinions:
     *
     *  1. The backend model. `risk_level` only exists on a FINAL answer. When the model instead replies
     *     with a question or consent request it has recognised a risk topic (a code, a payment, gift
     *     cards ...) and wants the user's story; in testing every such notification was a real scam,
     *     so that counts as SUSPICIOUS (it used to count as "verify").
     *  2. [heuristicRisk]: local rules for common scams the model was not trained on (see below).
     */
    private fun mapRisk(chatResult: ChatResult, preview: NotificationPreview): NotificationRisk {
        val answer = chatResult.response.answer
        val modelRisk = if (answer == null) {
            NotificationRisk.SUSPICIOUS
        } else {
            val riskLevel = answer.risk_level.orEmpty().lowercase(Locale.ROOT)
            val primaryAction = answer.primary_action.orEmpty().lowercase(Locale.ROOT)
            when {
                riskLevel == "critical" || riskLevel == "high" || answer.escalation_required -> NotificationRisk.HIGH_RISK
                riskLevel == "medium" -> NotificationRisk.SUSPICIOUS
                riskLevel == "low" || primaryAction.contains("verify") -> NotificationRisk.VERIFY
                else -> NotificationRisk.SAFE
            }
        }
        return moreSevere(modelRisk, heuristicRisk(preview))
    }

    private fun severity(r: NotificationRisk) = when (r) {
        NotificationRisk.HIGH_RISK -> 3
        NotificationRisk.SUSPICIOUS -> 2
        NotificationRisk.VERIFY -> 1
        else -> 0
    }

    private fun moreSevere(a: NotificationRisk, b: NotificationRisk) = if (severity(b) > severity(a)) b else a

    private fun unreachableRisk(p: NotificationPreview): NotificationRisk =
        heuristicRisk(p).takeIf { it != NotificationRisk.SAFE } ?: NotificationRisk.UNKNOWN

    // ---- local rules -------------------------------------------------------------------------------
    // The backend model was trained on simulated "user asks about a message" cases. Tested on bare
    // notifications it missed generic phishing ("account suspended, verify now: <link>", "install this
    // app to secure your account") and Finnish "tell us the code" texts. These rules cover that gap.
    // All patterns are lower-case and cover English / Finnish / Swedish.

    private val codeRequestRx = Regex(
        """(share|send|read|give|tell|forward|provide|reply with)\b.{0,30}\b(code|pin|otp|passcode)|""" +
            """(kerro|lähetä|anna|vastaa)\b.{0,40}(koodi|tunnusluku|pin)|""" +
            """(dela|skicka|ge|berätta)\b.{0,30}\b(kod|koden|pin)"""
    )
    // genuine one-time-code messages say "never share this code": do not flag those
    private val codeWarningRx = Regex(
        """(never|do not|don't|dont|älä|ei koskaan|aldrig|dela inte|ge inte)\s.{0,25}""" +
            """(share|give|tell|reveal|disclose|kerro|jaa|anna|luovuta|dela|ge|avslöja)"""
    )
    private val giftCardRx = Regex(
        """(buy|purchase|get|osta|hanki|köp).{0,30}(gift ?cards?|lahjakort\w*|presentkort)|""" +
            """(gift ?cards?|lahjakort\w*|presentkort).{0,40}(codes?|koodi\w*|koder)"""
    )
    private val installRx = Regex("""\b(install|download|asenna|lataa|installera|ladda ner)\b|\.apk""")
    private val credentialRx = Regex(
        """password|salasana|lösenord|card (details|number)|kortin? (tiedot|numero)|kortuppgifter|bank details|""" +
            """tilitiedot|pankkitunnus|bankid|personal (details|information)|henkilötiedot|personuppgifter"""
    )
    private val urgencyRx = Regex(
        """urgent|immediately|suspended|locked|compromised|unauthori[sz]ed|verify|confirm|act now|within \d+ ?(hours|h)|""" +
            """kiireellinen|heti|vahvista|lukittu|keskeytetty|sulj\w+|""" +
            """brådskande|omedelbart|bekräfta|låst|spärrad"""
    )

    /** Rule-based opinion. SAFE here means "no rule matched", not "verified safe". */
    private fun heuristicRisk(p: NotificationPreview): NotificationRisk {
        val lower = "${p.title} ${p.content}".lowercase(Locale.ROOT)
        val hasLink = URL_REGEX.containsMatchIn(lower)

        val asksForCode = codeRequestRx.containsMatchIn(lower) && !codeWarningRx.containsMatchIn(lower)
        return when {
            asksForCode -> NotificationRisk.HIGH_RISK                                 // SIM-swap / account takeover
            giftCardRx.containsMatchIn(lower) -> NotificationRisk.HIGH_RISK           // CEO / "boss" gift-card fraud
            hasLink && installRx.containsMatchIn(lower) -> NotificationRisk.SUSPICIOUS // malicious app install
            hasLink && credentialRx.containsMatchIn(lower) -> NotificationRisk.SUSPICIOUS // credential / card phishing
            hasLink && urgencyRx.containsMatchIn(lower) -> NotificationRisk.VERIFY    // pressure + link
            else -> NotificationRisk.SAFE
        }
    }
}
