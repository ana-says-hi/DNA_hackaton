package com.example.dna_mock

import com.google.gson.annotations.SerializedName

// --- UI Message State Model ---

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val replyType: String? = null, // "question", "consent_request", or "final"
    val steps: List<String> = emptyList(),
    val escalationRequired: Boolean = false,
    val escalationTarget: String? = null,
    val consentRequestedFor: List<String> = emptyList(),
    val rawJson: String? = null
)

// --- Consent & Signals Models ---

data class ConsentIn(
    val granted: List<String> = emptyList(),
    val refused: List<String> = emptyList()
)

data class SignalsInput(
    val artifact: Map<String, Any>? = null,
    val link_check: Map<String, Any>? = null,
    val call_warning: Map<String, Any>? = null,
    val subscription: Map<String, Any>? = null
)

// --- Request Models ---

data class ChatRequest(
    val message: String,
    val session_id: String? = null,
    val reset: Boolean = false,
    val language: String? = null,
    val journey: String? = null,
    val audience: String? = null,
    val signals: SignalsInput? = null,
    val pre_granted: List<String>? = null,
    val consent: ConsentIn? = null
)

data class ResetRequest(
    val session_id: String
)

data class ResetResponse(
    val ok: Boolean
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

data class ChatResponse(
    val session_id: String,
    val reply_type: String,
    val display_text: String,
    val language: String,
    val journey: String? = null,
    val question: Map<String, Any>? = null,
    val consent_request: Map<String, Any>? = null,
    val answer: FinalAnswerPayload? = null,
    val tier_used: String? = null,
    val safety_rules: List<String>? = null
)

data class ChatResult(
    val response: ChatResponse,
    val rawJson: String
)