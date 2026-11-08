package com.example.dna_mock

import com.google.gson.JsonObject

const val DEFAULT_CHAT_TITLE = "DNA Security Assistant"

// NOTE: every persisted model below gives ALL constructor parameters a default value.
// That makes Kotlin generate a no-arg constructor, so Gson can restore old history files
// after you add new fields (missing fields get their default instead of null).

// --- UI Message State Model ---

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val isUser: Boolean = false,
    /** "question", "consent_request", "final", or "note" (local system note, not from the server). */
    val replyType: String? = null,
    val language: String = "en",
    val steps: List<String> = emptyList(),
    val riskLevel: String? = null,
    val escalationRequired: Boolean = false,
    val escalationTarget: String? = null,
    /** Signal paths the server asked consent for, e.g. "subscription.recent_events". */
    val consentRequestedFor: List<String> = emptyList(),
    /** True once the user tapped Allow / Don't allow (buttons are then disabled). */
    val consentAnswered: Boolean = false,
    val isError: Boolean = false,
    val rawJson: String? = null
)

// --- Conversation (one entry in the history drawer) ---

data class Conversation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = DEFAULT_CHAT_TITLE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    /** Server session for this conversation. Each conversation owns its own session. */
    val sessionId: String? = null,
    /** Last successful server contact; used to detect the server's 1-hour session expiry. */
    val sessionTouchedAt: Long = 0L,
    val language: String = "en",
    /** Demo scenario whose signals are sent with the first message of a (new) session. */
    val scenarioId: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

/** Wrapper written to disk so the file format can evolve. */
data class HistoryFile(
    val version: Int = 1,
    val conversations: List<Conversation> = emptyList()
)

// --- Demo scenarios (operator-side signals) ---

/**
 * A bundle of operator-side signals (call warning, subscription events, device findings,
 * account logins ...) taken from data/dev/cases.jsonl. Loaded from assets/demo_scenarios.json.
 * In production these come from your operator backend instead of a JSON asset.
 */
data class DemoScenario(
    val id: String = "",
    val title: String = "",
    val language: String? = null,
    val audience: String? = null,
    val opening_message: String = "",
    val pre_granted: List<String> = emptyList(),
    val signals: JsonObject? = null
)

// --- Consent model ---

data class ConsentIn(
    val granted: List<String> = emptyList(),
    val refused: List<String> = emptyList()
)

// --- Request Models ---

data class ChatRequest(
    val message: String,
    val session_id: String? = null,
    val reset: Boolean = false,
    val language: String? = null,
    val journey: String? = null,
    val audience: String? = null,
    /**
     * Free-form on purpose: the server accepts any signal group (artifact, link_check,
     * call_warning, subscription, device, operator_account, credential_exposure, business ...).
     * The old 4-field SignalsInput silently dropped device / operator_account / credential_exposure,
     * which are exactly the groups that trigger consent requests.
     */
    val signals: JsonObject? = null,
    val pre_granted: List<String>? = null,
    val consent: ConsentIn? = null
)

data class ResetRequest(
    val session_id: String
)

data class ResetResponse(
    val ok: Boolean = false
)

// --- Response Models ---

data class FinalAnswerPayload(
    val case_id: String? = null,
    val risk_level: String? = null,
    val primary_action: String? = null,
    val advice_text: String? = null,
    val escalation_required: Boolean = false,
    val escalation_target: String? = null,
    val urgency: String? = null,
    val steps: List<String> = emptyList(),
    val questions_asked: List<String> = emptyList(),
    val question_topics: List<String> = emptyList(),
    val signals_used: List<String> = emptyList(),
    val consent_requested_for: List<String> = emptyList(),
    val citations: List<String> = emptyList(),
    val confidence: Float? = null,
    val abstained: Boolean = false
)

data class QuestionPayload(
    val text: String? = null,
    val topic: String? = null
)

/** `fields` are the exact signal paths to send back in ConsentIn.granted / refused. */
data class ConsentRequestPayload(
    val fields: List<String> = emptyList(),
    val text: String? = null
)

data class ChatResponse(
    val session_id: String = "",
    val reply_type: String = "final",
    val display_text: String = "",
    val language: String = "en",
    val journey: String? = null,
    val question: QuestionPayload? = null,
    val consent_request: ConsentRequestPayload? = null,
    val answer: FinalAnswerPayload? = null,
    val tier_used: String? = null,
    val safety_rules: List<String>? = null
)

/** [rawJson] is the server's response body, pretty-printed, exactly as received. */
data class ChatResult(
    val response: ChatResponse,
    val rawJson: String
)
