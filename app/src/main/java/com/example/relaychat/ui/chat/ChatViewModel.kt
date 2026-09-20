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


data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
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
                }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isEmpty() || state.isSending) return

        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                repository.sendMessage(conversationId, myUid, otherUid, text)
                _uiState.update { it.copy(inputText = "", isSending = false) }
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
}

class ChatViewModelFactory(
    private val myUid: String,
    private val otherUid: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(myUid, otherUid) as T
}
