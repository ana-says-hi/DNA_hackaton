package com.example.dna_mock

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SecurityApi {
    @POST("api/chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatResponse>
}