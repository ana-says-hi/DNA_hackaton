package com.example.dna_mock.services

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationManagerCompat
import com.example.dna_mock.NotificationRisk

/** Temporary on-device previews only; nothing is written to disk or uploaded. */
data class NotificationPreview(
    val key: String,
    val appName: String,
    val title: String,
    val content: String,
    val postedAt: Long,
    val risk: NotificationRisk
)

object NotificationInbox {
    val items = mutableStateListOf<NotificationPreview>()
    fun replace(next: List<NotificationPreview>) {
        items.clear()
        items.addAll(next)
    }

    fun suspiciousNotifications(): List<NotificationPreview> =
        items.filter { it.risk == NotificationRisk.SUSPICIOUS }

    fun updateRisk(
        key: String,
        risk: NotificationRisk
    ) {
        val index = items.indexOfFirst { it.key == key }

        if (index != -1) {
            items[index] = items[index].copy(
                risk = risk
            )
        }
    }
}

fun isNotificationReaderEnabled(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Android only connects this service after the user opts in through system Settings. */
class NotificationReaderService : NotificationListenerService() {
    companion object {
        private var connected: NotificationReaderService? = null
        fun refreshIfConnected() {
            connected?.refreshActiveNotifications()
        }
    }

    private fun refreshActiveNotifications() {
        val active = runCatching { activeNotifications?.toList().orEmpty() }
            .getOrDefault(emptyList())
        val previews = active.asSequence()
            //.filter { it.packageName != packageName }  //commented for high risk demo
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .mapNotNull { it.asPreview() }
            .sortedByDescending { it.postedAt }
            .take(30)
            .toList()

        val analysedNotifications = analyseNotifications(previews)

        Handler(Looper.getMainLooper()).post {
            NotificationInbox.replace(analysedNotifications)
        }}

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
            risk = if (title.contains("Urgent", ignoreCase = true) ||
                content.contains("Suspicious", ignoreCase = true) ||
                content.contains("verify", ignoreCase = true)) {
                NotificationRisk.SUSPICIOUS
            } else {
                NotificationRisk.UNKNOWN
            }
        )
    }

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
        NotificationInbox.replace(emptyList())
        super.onDestroy()
    }

    private fun analyseNotifications(
        notifications: List<NotificationPreview>
    ): List<NotificationPreview> {

        // TODO:
        // Send these notifications to the AI service.
        //
        // The AI should determine whether each notification is:
        // - SAFE
        // - SUSPICIOUS
        //
        // Example future API:
        //
        // val analysed = chatService.analyseNotifications(notifications)
        // return analysed
        //
        // For now, return the original list.

        return notifications
    }
}
