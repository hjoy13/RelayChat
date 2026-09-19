package com.example.relaychat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.relaychat.data.repository.AuthRepository
import com.example.relaychat.data.repository.UserRepository
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
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = authRepository.getCurrentUser() != null
        )
    )

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(
        displayName: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(
                isLoading = true
            )

            authRepository.register(email, password)
                .onSuccess { firebaseUser ->

                    val profileResult = userRepository.createUserProfile(
                        uid = firebaseUser.uid,
                        displayName = displayName,
                        email = firebaseUser.email ?: email
                    )

                    profileResult
                        .onSuccess {
                            _uiState.value = AuthUiState(
                                isLoggedIn = true
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = AuthUiState(
                                errorMessage = error.message
                                    ?: "Failed to create user profile."
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message
                            ?: "Registration failed."
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

            authRepository.login(email, password)
                .onSuccess { firebaseUser ->

                    val profileResult = userRepository.ensureUserProfile(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: email
                    )

                    profileResult
                        .onSuccess {
                            _uiState.value = AuthUiState(
                                isLoggedIn = true
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = AuthUiState(
                                errorMessage = error.message
                                    ?: "Failed to prepare user profile."
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message
                            ?: "Login failed."
                    )
                }
        }
    }

    fun logout() {
        authRepository.logout()

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