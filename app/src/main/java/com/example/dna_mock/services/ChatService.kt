package com.example.dna_mock.services

import com.example.dna_mock.ChatRequest
import com.example.dna_mock.ChatResponse
import com.example.dna_mock.ChatResult
import com.example.dna_mock.ConsentIn
import com.example.dna_mock.ResetRequest
import com.example.dna_mock.ResetResponse
import com.example.dna_mock.SignalsInput
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- Retrofit API Interface ---

interface SecurityApi {
    @POST("api/chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatResponse>

    @POST("api/reset")
    suspend fun resetSession(@Body request: ResetRequest): Response<ResetResponse>
}

// --- Chat Service Implementation ---

class ChatService(baseUrl: String = "http://10.20.1.234:8000/") {

    private val gson = Gson()
    private var currentSessionId: String? = null

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
     * Sends a chat message to api_server.py.
     * Automatically keeps track of session_id across chat turns.
     */
    suspend fun sendMessage(
        message: String,
        language: String? = null,
        journey: String? = null,
        signals: SignalsInput? = null,
        consent: ConsentIn? = null,
        reset: Boolean = false
    ): Result<ChatResult> {
        return try {
            val request = ChatRequest(
                message = message,
                session_id = if (reset) null else currentSessionId,
                reset = reset,
                language = language,
                journey = journey,
                signals = signals,
                consent = consent
            )

            val response = api.sendChatMessage(request)

            if (response.isSuccessful && response.body() != null) {
                val chatResponse = response.body()!!

                // Persist session_id across turns
                currentSessionId = chatResponse.session_id

                val rawJsonString = gson.toJson(chatResponse)
                Result.success(ChatResult(chatResponse, rawJsonString))
            } else {
                Result.failure(Exception("Server error: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resets the active session on the backend.
     */
    suspend fun resetSession(): Result<Boolean> {
        val sessionIdToReset = currentSessionId ?: return Result.success(true)
        return try {
            val response = api.resetSession(ResetRequest(sessionIdToReset))
            if (response.isSuccessful && response.body()?.ok == true) {
                currentSessionId = null
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to reset session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}