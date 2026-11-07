package com.example.dna_mock.data

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- Data Models required by the API ---

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

data class ChatResult(
    val parsedResponse: ChatResponse,
    val rawJson: String
)

// --- Retrofit API Interface ---

interface SecurityApi {
    @POST("api/chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatResponse>
}

// --- Chat Service Implementation ---

class ChatService(baseUrl: String = "http://10.20.1.234:8000/") {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val api: SecurityApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(SecurityApi::class.java)

    /**
     * Sends a message to the backend and returns both the parsed object and raw JSON string.
     */
    suspend fun sendMessage(
        message: String,
        language: String? = "auto",
        journey: String? = null
    ): Result<ChatResult> {
        return try {
            val request = ChatRequest(
                message = message,
                language = language,
                journey = journey
            )

            val response = api.sendChatMessage(request)

            if (response.isSuccessful && response.body() != null) {
                val chatResponse = response.body()!!
                val rawJsonString = gson.toJson(chatResponse)

                Result.success(ChatResult(chatResponse, rawJsonString))
            } else {
                Result.failure(Exception("Server error: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}