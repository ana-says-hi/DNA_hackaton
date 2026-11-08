package com.example.dna_mock.services

import com.example.dna_mock.ChatRequest
import com.example.dna_mock.ChatResponse
import com.example.dna_mock.ChatResult
import com.example.dna_mock.ConsentIn
import com.example.dna_mock.ResetRequest
import com.example.dna_mock.ResetResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

// --- Retrofit API Interface ---

interface SecurityApi {
    // ResponseBody (not ChatResponse) so we keep the server's real JSON for the "View Raw JSON" dialog.
    @POST("api/chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ResponseBody>

    @POST("api/reset")
    suspend fun resetSession(@Body request: ResetRequest): Response<ResetResponse>
}

// --- Chat Service Implementation ---

/**
 * Thin, STATELESS client for api_server.py.
 *
 * It no longer remembers a session id. Each conversation in the app owns its own session id
 * (the ViewModel passes it in and stores the one returned), so switching between chats in the
 * history drawer continues the right server-side conversation.
 *
 * @param baseUrl must end with "/". Android emulator -> "http://10.0.2.2:8000/".
 * @param debugLogging logs full request/response bodies. Keep OFF in release: bodies contain
 *        the user's operator-side signals (account logins, device findings ...).
 */
class ChatService(
    baseUrl: String = DEFAULT_BASE_URL,
    debugLogging: Boolean = false
) {
    companion object {
        const val DEFAULT_BASE_URL = "http://10.51.4.60:8000/"
    }

    private val gson = Gson()
    private val prettyGson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // the optional Gemini tier can be slower than the local model
        .apply {
            if (debugLogging) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            }
        }
        .build()

    private val api: SecurityApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(SecurityApi::class.java)

    /**
     * Sends one chat turn.
     *
     * @param sessionId session to continue; null starts a new session.
     * @param signals operator-side signals. Only read by the server when it CREATES a session,
     *        so pass them with the first message of a session (and again after a session restart).
     * @param consent explicit Allow / Don't allow answer for a pending consent_request.
     */
    suspend fun sendMessage(
        message: String,
        sessionId: String?,
        language: String? = null,
        journey: String? = null,
        audience: String? = null,
        signals: JsonObject? = null,
        preGranted: List<String>? = null,
        consent: ConsentIn? = null,
        reset: Boolean = false
    ): Result<ChatResult> {
        return try {
            val request = ChatRequest(
                message = message,
                session_id = if (reset) null else sessionId,
                reset = reset,
                language = language,
                journey = journey,
                audience = audience,
                signals = signals,
                pre_granted = preGranted,
                consent = consent
            )

            val response = api.sendChatMessage(request)
            val body = if (response.isSuccessful) response.body()?.string() else null

            if (body != null) {
                val chatResponse = gson.fromJson(body, ChatResponse::class.java)
                Result.success(ChatResult(chatResponse, prettyPrint(body)))
            } else {
                Result.failure(IOException(describeHttpError(response)))
            }
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: Exception) {
            Result.failure(IOException(describeException(e), e))
        }
    }

    /** Forgets a session on the server. Safe to call for unknown / expired sessions. */
    suspend fun resetSession(sessionId: String): Result<Boolean> {
        return try {
            val response = api.resetSession(ResetRequest(sessionId))
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(true)
            } else {
                Result.failure(IOException("Failed to reset session"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- helpers ---

    private fun prettyPrint(json: String): String = try {
        prettyGson.toJson(gson.fromJson(json, JsonElement::class.java)) // works on every Gson version
    } catch (e: Exception) {
        json
    }

    private fun describeHttpError(response: Response<ResponseBody>): String {
        val raw = try { response.errorBody()?.string() } catch (e: Exception) { null }
        // FastAPI errors look like {"detail": "..."} (or a list for 422 validation errors)
        val detail = try {
            raw?.let { gson.fromJson(it, JsonObject::class.java).get("detail")?.toString()?.trim('"') }
        } catch (e: Exception) { null }
        return "Server error ${response.code()}: ${detail ?: raw ?: response.message()}"
    }

    private fun describeException(e: Exception): String = when (e) {
        is UnknownHostException, is ConnectException ->
            "Can't reach the assistant server. Check the network and the server address."
        is SocketTimeoutException -> "The assistant server took too long to answer."
        else -> e.localizedMessage ?: "Unexpected error"
    }
}
