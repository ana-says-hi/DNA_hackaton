package com.example.dna_mock

data class ArtifactSignal(
    val body: String? = "",
    val sender: String? = ""
)

data class SignalsInput(
    val artifact: ArtifactSignal? = null,
    val link_check: Map<String, Any>? = null,
    val subscription: Map<String, Any>? = null
)

data class ChatRequest(
    val message: String,
    val case_id: String? = null,
    val language: String? = "auto",
    val journey: String? = null,
    val signals: SignalsInput? = null,
    val use_gemini_fallback: Boolean = true
)

data class ChatResponse(
    val case_id: String,
    val language_used: String,
    val risk_level: String,
    val primary_action: String,
    val advice_text: String,
    val escalation_required: Boolean,
    val escalation_target: String?,
    val urgency: String,
    val steps: List<String>,
    val questions_asked: List<String>,
    val question_topics: List<String>,
    val signals_used: List<String>,
    val consent_requested_for: List<String>,
    val citations: List<String>,
    val confidence: Float,
    val abstained: Boolean,
    val tier_used: String
)

// Wrapper container to hold both parsed data and raw JSON for your UI inspector
data class ChatResult(
    val parsedResponse: ChatResponse,
    val rawJson: String
)