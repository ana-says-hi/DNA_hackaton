package com.example.dna_mock.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dna_mock.ChatMessage
import com.example.dna_mock.ChatResult
import com.example.dna_mock.ConsentIn
import com.example.dna_mock.SignalsInput
import com.example.dna_mock.services.ChatService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatHistoryItem(
    val id: String,
    val title: String
)

class ChatViewModel(private val chatService: ChatService = ChatService()) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // UI Input State
    var inputText by mutableStateOf("")
        private set

    // TopAppBar & History State
    var chatTitle by mutableStateOf("DNA Security Assistant")
        private set

    var conversationId by mutableStateOf("default_conv")
        private set

    var chatHistory by mutableStateOf<List<ChatHistoryItem>>(emptyList())
        private set

    // Raw JSON Dialog State
    var isJsonDialogOpen by mutableStateOf(false)
        private set

    var selectedRawJson by mutableStateOf<String?>(null)
        private set

    fun updateInputText(newText: String) {
        inputText = newText
    }

    fun sendMessage() {
        val messageText = inputText.trim()
        if (messageText.isEmpty()) return

        // 1. Add User Message
        val userMsg = ChatMessage(text = messageText, isUser = true)
        _messages.value = _messages.value + userMsg
        inputText = ""

        // 2. Call API
        viewModelScope.launch {
            val result: Result<ChatResult> = chatService.sendMessage(message = messageText)
            result.onSuccess { chatResult ->
                val response = chatResult.response
                val answerPayload = response.answer

                val aiMsg = ChatMessage(
                    text = response.display_text,
                    isUser = false,
                    replyType = response.reply_type,
                    steps = answerPayload?.steps ?: emptyList(),
                    escalationRequired = answerPayload?.escalation_required ?: false,
                    escalationTarget = answerPayload?.escalation_target,
                    consentRequestedFor = answerPayload?.consent_requested_for ?: emptyList(),
                    rawJson = chatResult.rawJson
                )
                _messages.value = _messages.value + aiMsg
            }.onFailure { error: Throwable ->
                val errorMsg = ChatMessage(
                    text = "Error: ${error.localizedMessage ?: "Failed to connect to backend"}",
                    isUser = false
                )
                _messages.value = _messages.value + errorMsg
            }
        }
    }

    fun sendConsent(granted: Boolean, requestedFor: List<String>) {
        val consentInput = if (granted) {
            ConsentIn(granted = requestedFor)
        } else {
            ConsentIn(refused = requestedFor)
        }

        val userText = if (granted) "Allowed access" else "Denied access"
        _messages.value = _messages.value + ChatMessage(text = userText, isUser = true)

        viewModelScope.launch {
            val result: Result<ChatResult> = chatService.sendMessage(
                message = if (granted) "yes" else "no",
                consent = consentInput
            )
            result.onSuccess { chatResult ->
                val response = chatResult.response
                val answerPayload = response.answer

                val aiMsg = ChatMessage(
                    text = response.display_text,
                    isUser = false,
                    replyType = response.reply_type,
                    steps = answerPayload?.steps ?: emptyList(),
                    escalationRequired = answerPayload?.escalation_required ?: false,
                    escalationTarget = answerPayload?.escalation_target,
                    consentRequestedFor = answerPayload?.consent_requested_for ?: emptyList(),
                    rawJson = chatResult.rawJson
                )
                _messages.value = _messages.value + aiMsg
            }
        }
    }

    fun talkToPerson(target: String?) {
        val text = "Requesting agent support${target?.let { " ($it)" } ?: ""}"
        _messages.value = _messages.value + ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + ChatMessage(
            text = "Connecting you to security specialist ${target ?: "support team"}...",
            isUser = false
        )
    }

    fun newChat() {
        viewModelScope.launch {
            chatService.resetSession()
            _messages.value = emptyList()
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    fun openChat(chat: ChatHistoryItem) {
        conversationId = chat.id
        chatTitle = chat.title
    }

    fun showRawJson(json: String) {
        selectedRawJson = json
        isJsonDialogOpen = true
    }

    fun dismissJsonDialog() {
        isJsonDialogOpen = false
        selectedRawJson = null
    }
}