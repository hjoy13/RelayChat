package com.example.relaychat.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.relaychat.data.model.User
import com.example.relaychat.data.repository.AuthRepository
import com.example.relaychat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UsersUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val errorMessage: String? = null
)

class UsersViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UsersUiState(
            isLoading = true
        )
    )

    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UsersUiState(
                isLoading = true
            )

            userRepository.getUsers()
                .onSuccess { users ->
                    val currentUserId =
                        authRepository.getCurrentUser()?.uid

                    val otherUsers = users
                        .filter { user ->
                            user.uid != currentUserId
                        }
                        .sortedBy { user ->
                            user.displayName.lowercase()
                        }

                    _uiState.value = UsersUiState(
                        users = otherUsers
                    )
                }
                .onFailure { error ->
                    _uiState.value = UsersUiState(
                        errorMessage = error.message
                            ?: "Failed to load users."
                    )
                }
        }
    }
}