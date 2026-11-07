package com.example.dna_mock.data

import kotlinx.coroutines.delay

/**
 * Represents a single message in the chat.
 * @param text The content of the message.
 * @param isUser True if the message was sent by the user, false if by the AI.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

/**
 * A service to handle AI chatbot logic.
 * Currently simulates an API response with a delay.
 */
class ChatService {

    /**
     * Simulates sending a message to an AI and receiving a response.
     */
    suspend fun getResponse(userMessage: String): String {
        // Simulate network latency (1 second)
        delay(1000)

        // Placeholder logic for the AI response
        return when {
            userMessage.contains("hello", ignoreCase = true) ->
                "Hi there! How can I assist you with your account today?"
            userMessage.contains("topup", ignoreCase = true) ->
                "You can top up your balance by clicking the 'Topup' tab in the navigation bar."
            else ->
                "I received your message: '$userMessage'. I'm still learning, but I'll be able to help more soon!"
        }
    }
}