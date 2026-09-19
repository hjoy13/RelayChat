package com.example.relaychat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.relaychat.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = repository.getCurrentUser() != null
        )
    )

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(
                isLoading = true
            )

            repository.register(email, password)
                .onSuccess {
                    _uiState.value = AuthUiState(
                        isLoggedIn = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message ?: "Registration failed."
                    )
                }
        }
    }

    fun login(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(
                isLoading = true
            )

            repository.login(email, password)
                .onSuccess {
                    _uiState.value = AuthUiState(
                        isLoggedIn = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message ?: "Login failed."
                    )
                }
        }
    }

    fun logout() {
        repository.logout()

        _uiState.value = AuthUiState(
            isLoggedIn = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
}