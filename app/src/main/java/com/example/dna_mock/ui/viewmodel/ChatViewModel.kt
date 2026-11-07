package com.example.dna_mock.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dna_mock.data.ChatService
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val steps: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val rawJson: String? = null
)

class ChatViewModel : ViewModel() {
    private val chatService = ChatService() // Connects to http://10.0.2.2:8000/

    var messages by mutableStateOf(listOf(ChatMessage("Hello! How can I help you today with your security check?", false)))
        private set

    var inputText by mutableStateOf("")
        private set

    var chatTitle by mutableStateOf("New chat")
        private set

    var selectedRawJson by mutableStateOf<String?>(null)
        private set

    var isJsonDialogOpen by mutableStateOf(false)
        private set

    fun updateInputText(newText: String) {
        inputText = newText
    }

    fun showRawJson(json: String?) {
        selectedRawJson = json
        isJsonDialogOpen = true
    }

    fun dismissJsonDialog() {
        isJsonDialogOpen = false
        selectedRawJson = null
    }

    private fun updateTitle(userMsg: String) {
        if (messages.size <= 2) {
            chatTitle = if (userMsg.length > 15) userMsg.take(15) + "..." else userMsg
        }
    }

    fun sendMessage() {
        val userMsg = inputText.trim()
        if (userMsg.isBlank()) return

        messages = messages + ChatMessage(userMsg, true)
        inputText = ""
        updateTitle(userMsg)

        viewModelScope.launch {
            val result = chatService.sendMessage(message = userMsg)
            result.onSuccess { chatResult ->
                val response = chatResult.parsedResponse

                messages = messages + ChatMessage(
                    text = response.advice_text,
                    isUser = false,
                    steps = response.steps,
                    questions = response.questions_asked,
                    rawJson = chatResult.rawJson
                )
            }.onFailure { error ->
                messages = messages + ChatMessage(
                    text = "Connection error: ${error.localizedMessage}",
                    isUser = false
                )
            }
        }
    }
}