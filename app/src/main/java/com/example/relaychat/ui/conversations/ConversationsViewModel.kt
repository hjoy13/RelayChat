package com.example.relaychat.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.relaychat.data.repository.ChatRepository
import com.example.relaychat.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationItem(
    val conversationId: String,
    val otherUid: String,
    val otherName: String,
    val lastMessage: String,
    val lastMessageAt: Timestamp?,
    val lastSenderId: String,
    val isUnread: Boolean
)

data class ConversationsUiState(
    val items: List<ConversationItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ConversationsViewModel(
    private val myUid: String,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    // uid -> display name, so each person is looked up only once.
    private val nameCache = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            chatRepository.observeConversations(myUid)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Could not load conversations"
                        )
                    }
                }
                .collectLatest { conversations ->
                    val items = conversations.mapNotNull { conversation ->
                        val otherUid = conversation.memberIds.firstOrNull { it != myUid }
                            ?: return@mapNotNull null

                        val myLastReadAt = conversation.lastReadAt?.get(myUid)?.toDate()?.time ?: 0L
                        val lastMessageTime = conversation.lastMessageAt?.toDate()?.time ?: 0L
                        val isUnread = conversation.lastSenderId != myUid && lastMessageTime > myLastReadAt

                        ConversationItem(
                            conversationId = conversation.id,
                            otherUid = otherUid,
                            otherName = nameFor(otherUid),
                            lastMessage = conversation.lastMessage,
                            lastMessageAt = conversation.lastMessageAt,
                            lastSenderId = conversation.lastSenderId,
                            isUnread = isUnread
                        )
                    }
                    _uiState.update {
                        it.copy(items = items, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    private suspend fun nameFor(uid: String): String {
        nameCache[uid]?.let { return it }
        val user = userRepository.getUser(uid).getOrNull()
        val name = user?.displayName?.takeIf { it.isNotBlank() } ?: return "Unknown user"
        nameCache[uid] = name
        return name
    }
}

class ConversationsViewModelFactory(
    private val myUid: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ConversationsViewModel(myUid) as T
}