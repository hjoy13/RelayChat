package com.example.relaychat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.relaychat.data.model.Message
import com.example.relaychat.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val replyingTo: Message? = null,
    val isOtherTyping: Boolean = false,
    val otherLastReadAt: Long = 0L
)

class ChatViewModel(
    private val myUid: String,
    private val otherUid: String,
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    val conversationId: String = repository.conversationIdFor(myUid, otherUid)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var opened = false
            var attempts = 0

            // Retry a few times: right after login the connection may not be ready yet.
            while (!opened && attempts < 3) {
                try {
                    repository.openOrCreateConversation(myUid, otherUid)
                    opened = true
                } catch (e: Exception) {
                    attempts++
                    if (attempts >= 3) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Could not open conversation"
                            )
                        }
                        return@launch
                    }
                    delay(2000)
                }
            }

            repository.observeMessages(conversationId)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Could not load messages")
                    }
                }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                    if (messages.isNotEmpty()) {
                        repository.markAsRead(conversationId, myUid)
                    }
                }
        }
        viewModelScope.launch {
            repository.observeTyping(conversationId, otherUid)
                .collect { typing ->
                    _uiState.update { it.copy(isOtherTyping = typing) }
                }
        }
        viewModelScope.launch {
            repository.observeOtherLastRead(conversationId, otherUid)
                .collect { lastReadAt ->
                    _uiState.update { it.copy(otherLastReadAt = lastReadAt) }
                }
        }
    }

    private var typingJob: Job? = null

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }

        typingJob?.cancel()
        viewModelScope.launch {
            repository.setTyping(conversationId, myUid, true)
        }
        typingJob = viewModelScope.launch {
            delay(3000)
            repository.setTyping(conversationId, myUid, false)
        }
    }


    fun setReplyTarget(message: Message) {
        _uiState.update { it.copy(replyingTo = message) }
    }

    fun clearReplyTarget() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isEmpty() || state.isSending) return

        val replyTo = state.replyingTo
        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        typingJob?.cancel()
        viewModelScope.launch {
            repository.setTyping(conversationId, myUid, false)
        }

        viewModelScope.launch {
            try {
                repository.sendMessage(
                    conversationId = conversationId,
                    senderId = myUid,
                    receiverId = otherUid,
                    text = text,
                    replyToMessageId = replyTo?.id,
                    replyToText = replyTo?.text,
                    replyToSenderId = replyTo?.senderId
                )
                _uiState.update { it.copy(inputText = "", isSending = false, replyingTo = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSending = false, errorMessage = e.message ?: "Message failed to send")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            repository.setTyping(conversationId, myUid, false)
        }
    }
}

class ChatViewModelFactory(
    private val myUid: String,
    private val otherUid: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(myUid, otherUid) as T
}
