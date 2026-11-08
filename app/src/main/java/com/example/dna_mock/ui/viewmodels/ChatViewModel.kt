package com.example.dna_mock.ui.viewmodels

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dna_mock.ChatMessage
import com.example.dna_mock.ChatResponse
import com.example.dna_mock.ConsentIn
import com.example.dna_mock.Conversation
import com.example.dna_mock.DEFAULT_CHAT_TITLE
import com.example.dna_mock.DemoScenario
import com.example.dna_mock.data.ChatHistoryRepository
import com.example.dna_mock.data.DemoScenarioRepository
import com.example.dna_mock.services.ChatService
import com.example.dna_mock.ui.UiStrings
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ChatHistoryItem(
    val id: String,
    val title: String,
    val updatedAt: Long = 0L
)

/**
 * Single source of truth: [_conversations]. Every conversation owns
 *  - its messages,
 *  - its own server session id (so resuming a chat from history continues the same session),
 *  - the demo scenario (operator signals) attached to it.
 *
 * The visible chat ([messages], [chatTitle] ...) is just a view of the ACTIVE conversation.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val MAX_INPUT_CHARS = 4000                 // server: ChatRequest.message max_length
        const val SESSION_TTL_MS = 55 * 60 * 1000L       // server drops sessions after 60 min; stay safely below
        const val TITLE_MAX_CHARS = 40
    }

    private val isDebuggable =
        (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val chatService = ChatService(debugLogging = isDebuggable)
    private val historyRepo = ChatHistoryRepository(File(application.filesDir, "chat_history.json"))
    private val scenarioRepo = DemoScenarioRepository(application)

    // ---- core state -------------------------------------------------------------------

    /** Always contains the active conversation (an empty "draft" until the first message). */
    private val _conversations = MutableStateFlow(listOf(Conversation()))
    private val _activeId = MutableStateFlow(_conversations.value.first().id)
    private val _pending = MutableStateFlow<Set<String>>(emptySet())
    private val _scenarios = MutableStateFlow<List<DemoScenario>>(emptyList())
    private val jobs = mutableMapOf<String, Job>()      // main thread only

    private val activeConversation: StateFlow<Conversation> =
        combine(_conversations, _activeId) { list, id ->
            list.firstOrNull { it.id == id } ?: Conversation(id = id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, _conversations.value.first())

    // ---- state exposed to the UI --------------------------------------------------------

    val messages: StateFlow<List<ChatMessage>> =
        activeConversation.map { it.messages }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val chatTitle: StateFlow<String> =
        activeConversation.map { it.title }.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_CHAT_TITLE)

    val activeConversationId: StateFlow<String> = _activeId.asStateFlow()

    val activeLanguage: StateFlow<String> =
        activeConversation.map { it.language }.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    val selectedScenarioId: StateFlow<String?> =
        activeConversation.map { it.scenarioId }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** True while the ACTIVE conversation is waiting for the server. */
    val isLoading: StateFlow<Boolean> =
        combine(_pending, _activeId) { pending, id -> id in pending }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Non-empty conversations, newest first, for the drawer. */
    val chatHistory: StateFlow<List<ChatHistoryItem>> =
        _conversations.map { list ->
            list.filter { it.messages.isNotEmpty() }
                .sortedByDescending { it.updatedAt }
                .map { ChatHistoryItem(it.id, it.title, it.updatedAt) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val scenarios: StateFlow<List<DemoScenario>> = _scenarios.asStateFlow()

    // UI Input State
    var inputText by mutableStateOf("")
        private set

    // Raw JSON Dialog State
    var isJsonDialogOpen by mutableStateOf(false)
        private set

    var selectedRawJson by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            _scenarios.value = scenarioRepo.load()

            // restore history; keep whatever the user already started typing in the draft
            val saved = historyRepo.load()
            _conversations.update { current ->
                saved.filter { s -> current.none { it.id == s.id } } + current
            }

            // autosave (debounced). Started only AFTER loading so we never overwrite history with an empty list.
            _conversations.collectLatest { list ->
                delay(300)
                historyRepo.save(list)
            }
        }
    }

    // ---- input ---------------------------------------------------------------------------

    fun updateInputText(newText: String) {
        inputText = newText.take(MAX_INPUT_CHARS)
    }

    /**
     * Attach (or detach, with null) a demo scenario to the current EMPTY chat and prefill its
     * opening message. Its signals are sent with the first message, which is what lets the
     * server ask the user for consent.
     */
    fun selectScenario(scenario: DemoScenario?) {
        val id = _activeId.value
        val conv = _conversations.value.firstOrNull { it.id == id } ?: return
        if (conv.messages.isNotEmpty()) return // signals are only read when a session is created
        updateConversation(id) { it.copy(scenarioId = scenario?.id) }
        if (scenario != null) inputText = scenario.opening_message.take(MAX_INPUT_CHARS)
    }

    // ---- sending -------------------------------------------------------------------------

    fun sendMessage() {
        val text = inputText.trim()
        val convId = _activeId.value
        if (text.isEmpty() || convId in _pending.value) return
        inputText = ""

        updateConversation(convId) { c ->
            c.copy(
                title = if (c.messages.isEmpty()) makeTitle(text) else c.title,
                updatedAt = System.currentTimeMillis(),
                messages = c.messages + ChatMessage(text = text, isUser = true, language = c.language)
            )
        }
        dispatch(convId, wireText = text)
    }

    /**
     * Allow / Don't allow tapped on a consent_request bubble.
     * The signal paths come from the consent_request message (the server sends `answer = null`
     * for consent requests, so they can't be read from the final-answer payload).
     */
    fun sendConsent(messageId: String, granted: Boolean) {
        val convId = _activeId.value
        if (convId in _pending.value) return
        val conv = _conversations.value.firstOrNull { it.id == convId } ?: return
        val msg = conv.messages.firstOrNull { it.id == messageId } ?: return
        if (msg.consentAnswered) return

        val fields = msg.consentRequestedFor
        val consent = when {
            fields.isEmpty() -> null // server falls back to reading yes/no from the text
            granted -> ConsentIn(granted = fields)
            else -> ConsentIn(refused = fields)
        }

        // disable the buttons right away (prevents double taps); reverted if the request fails
        updateConversation(convId) { c ->
            c.copy(messages = c.messages.map { if (it.id == messageId) it.copy(consentAnswered = true) else it })
        }
        dispatch(
            convId,
            wireText = if (granted) "yes" else "no",
            consent = consent,
            userBubble = if (granted) UiStrings.allowedText(conv.language) else UiStrings.deniedText(conv.language),
            consentMessageId = messageId
        )
    }

    private fun dispatch(
        convId: String,
        wireText: String,
        consent: ConsentIn? = null,
        userBubble: String? = null,
        consentMessageId: String? = null
    ) {
        val conv = _conversations.value.firstOrNull { it.id == convId } ?: return
        val sentAt = System.currentTimeMillis()
        val startsNewSession = conv.sessionId == null || sentAt - conv.sessionTouchedAt > SESSION_TTL_MS
        val sessionExpired = startsNewSession && conv.sessionId != null
        val scenario = conv.scenarioId?.let { sid -> _scenarios.value.firstOrNull { it.id == sid } }

        _pending.update { it + convId }
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = chatService.sendMessage(
                    message = wireText,
                    sessionId = if (startsNewSession) null else conv.sessionId,
                    language = scenario?.language,
                    audience = scenario?.audience,
                    // signals + pre-granted consents are only read by the server when it creates a
                    // session, so send them only then (this also re-sends them after an expiry)
                    signals = if (startsNewSession) scenario?.signals else null,
                    preGranted = if (startsNewSession) scenario?.pre_granted?.takeIf { it.isNotEmpty() } else null,
                    consent = consent
                )
                result.onSuccess { chatResult ->
                    val resp = chatResult.response
                    val now = System.currentTimeMillis()
                    updateConversation(convId) { c ->
                        var msgs = c.messages
                        if (sessionExpired) {
                            msgs = msgs + ChatMessage(
                                text = UiStrings.sessionExpired(resp.language),
                                replyType = "note",
                                language = resp.language
                            )
                        }
                        if (userBubble != null) {
                            msgs = msgs + ChatMessage(text = userBubble, isUser = true, language = resp.language)
                        }
                        c.copy(
                            sessionId = resp.session_id,
                            sessionTouchedAt = now,
                            language = resp.language,
                            updatedAt = now,
                            messages = msgs + toAiMessage(resp, chatResult.rawJson)
                        )
                    }
                }.onFailure { error ->
                    updateConversation(convId) { c ->
                        c.copy(
                            messages = c.messages.map {
                                // give the user another chance to tap the consent buttons
                                if (it.id == consentMessageId) it.copy(consentAnswered = false) else it
                            } + ChatMessage(
                                text = UiStrings.error(c.language, error.localizedMessage),
                                isUser = false,
                                isError = true,
                                language = c.language
                            )
                        )
                    }
                }
            } finally {
                // only clean up if this job is still the current one (it may have been cancelled
                // by clear/delete and a newer request started for the same conversation)
                if (jobs[convId] === coroutineContext[Job]) {
                    jobs.remove(convId)
                    _pending.update { it - convId }
                }
            }
        }
        jobs[convId] = job   // registered BEFORE start so the finally-block identity check is reliable
        job.start()
    }

    private fun toAiMessage(resp: ChatResponse, rawJson: String): ChatMessage {
        val answer = resp.answer
        return ChatMessage(
            text = resp.display_text,
            isUser = false,
            replyType = resp.reply_type,
            language = resp.language,
            steps = answer?.steps.orEmpty(),
            riskLevel = answer?.risk_level,
            escalationRequired = answer?.escalation_required ?: false,
            escalationTarget = answer?.escalation_target,
            consentRequestedFor = resp.consent_request?.fields.orEmpty(),
            rawJson = rawJson
        )
    }

    // ---- escalation (mock hand-off) ---------------------------------------------------------

    fun talkToPerson(target: String?) {
        val id = _activeId.value
        updateConversation(id) { c ->
            c.copy(
                updatedAt = System.currentTimeMillis(),
                messages = c.messages + listOf(
                    ChatMessage(text = UiStrings.requestingSupport(c.language, target), isUser = true, language = c.language),
                    // TODO: replace with the real hand-off (open live-chat / call the agent queue)
                    ChatMessage(text = UiStrings.connecting(c.language, target), isUser = false, language = c.language)
                )
            )
        }
    }

    // ---- history: new / open / clear / delete -------------------------------------------------

    /**
     * Starts a fresh chat. The chat you leave is KEPT in history and keeps its server session,
     * so you can come back and continue it. (The old code reset the server session here,
     * which made returning to a previous chat impossible.)
     * If the current chat is already empty, nothing new is created.
     */
    fun newChat() {
        inputText = ""
        val active = _conversations.value.firstOrNull { it.id == _activeId.value }
        if (active != null && active.messages.isEmpty()) return
        startDraft()
    }

    /** Opens a chat from the history drawer. */
    fun openChat(chat: ChatHistoryItem) {
        if (_conversations.value.none { it.id == chat.id }) return
        inputText = ""
        activate(chat.id)
    }

    /**
     * Clears the CURRENT chat: stops any in-flight request, wipes its messages, and resets its
     * server session so the assistant really forgets the case (the old code only emptied the UI,
     * leaving stale context on the server). The emptied chat drops out of history.
     */
    fun clearChat() {
        val id = _activeId.value
        val conv = _conversations.value.firstOrNull { it.id == id } ?: return
        cancelRequests(id)
        resetRemote(conv.sessionId)
        updateConversation(id) { Conversation(id = it.id) }
        inputText = ""
    }

    /** Deletes one chat from history (and forgets its server session). */
    fun deleteChat(id: String) {
        val conv = _conversations.value.firstOrNull { it.id == id } ?: return
        cancelRequests(id)
        resetRemote(conv.sessionId)
        _conversations.update { list -> list.filterNot { it.id == id } }
        if (id == _activeId.value) {
            inputText = ""
            startDraft()
        }
    }

    /** Deletes the whole history and starts over with one empty chat. */
    fun deleteAllChats() {
        _conversations.value.forEach { c ->
            cancelRequests(c.id)
            resetRemote(c.sessionId)
        }
        inputText = ""
        val draft = Conversation()
        _conversations.value = listOf(draft)
        _activeId.value = draft.id
    }

    // ---- raw JSON dialog -----------------------------------------------------------------------

    fun showRawJson(json: String) {
        selectedRawJson = json
        isJsonDialogOpen = true
    }

    fun dismissJsonDialog() {
        isJsonDialogOpen = false
        selectedRawJson = null
    }

    // ---- helpers -------------------------------------------------------------------------------

    private fun startDraft() {
        val draft = Conversation()
        _conversations.update { it + draft }
        activate(draft.id)
    }

    /** Makes [id] the visible chat and discards other chats that never received a message. */
    private fun activate(id: String) {
        _activeId.value = id
        _conversations.update { list -> list.filter { it.messages.isNotEmpty() || it.id == id } }
    }

    private fun updateConversation(id: String, transform: (Conversation) -> Conversation) {
        _conversations.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun cancelRequests(id: String) {
        jobs.remove(id)?.cancel()
        _pending.update { it - id }
    }

    /** Best-effort server cleanup; failures are harmless because the server expires sessions itself. */
    private fun resetRemote(sessionId: String?) {
        if (sessionId == null) return
        viewModelScope.launch { chatService.resetSession(sessionId) }
    }

    private fun makeTitle(text: String): String {
        val oneLine = text.replace(Regex("\\s+"), " ").trim()
        return if (oneLine.length > TITLE_MAX_CHARS) oneLine.take(TITLE_MAX_CHARS).trimEnd() + "…" else oneLine
    }
}
