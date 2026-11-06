
package com.ecosphere.wastepickup.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecosphere.wastepickup.auth.remote.dto.loginRequest
import com.ecosphere.wastepickup.auth.repository.AuthRepository
import com.ecosphere.wastepickup.auth.repository.AuthResult
import com.ecosphere.wastepickup.core.security.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val identityError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false
)

sealed interface LoginUiEvent {
    data class Success(val userName: String) : LoginUiEvent
    data class ShowMessage(val message: String) : LoginUiEvent
}

class LoginViewModel(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _events =
        MutableSharedFlow<LoginUiEvent>(extraBufferCapacity = 1)

    val events = _events.asSharedFlow()

    fun login(identity: String, password: String) {
        if (_uiState.value.isLoading) return

        val cleanIdentity = identity.trim()

        val emailRegex =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        val mobileRegex =
            Regex("^\\+?[0-9]{10,15}$")

        val identityError = when {
            cleanIdentity.isEmpty() ->
                "Email or mobile number is required"

            !emailRegex.matches(cleanIdentity) &&
                    !mobileRegex.matches(cleanIdentity) ->
                "Enter a valid email or mobile number"

            else -> null
        }

        val passwordError = when {
            password.isEmpty() ->
                "Password is required"

            password.length < 8 ->
                "Password must contain at least 8 characters"

            else -> null
        }

        _uiState.update {
            it.copy(
                identityError = identityError,
                passwordError = passwordError
            )
        }

        if (identityError != null || passwordError != null) {
            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(isLoading = true)
            }

            when (
                val result = repository.login(
                    loginRequest(
                        emailOrMobile = cleanIdentity,
                        password = password
                    )
                )
            ) {

                is AuthResult.Success -> {

                    try {
                        tokenManager.saveToken(result.data.token)
                        tokenManager.saveUser(
                            id = result.data.user.id,
                            name = result.data.user.name,
                            email = result.data.user.email,
                            mobile = result.data.user.mobile
                        )

                        _uiState.update {
                            it.copy(isLoading = false)
                        }

                        _events.emit(
                            LoginUiEvent.Success(
                                result.data.user.name
                            )
                        )

                    } catch (e: Exception) {

                        _uiState.update {
                            it.copy(isLoading = false)
                        }

                        _events.emit(
                            LoginUiEvent.ShowMessage(
                                "Unable to securely save your session."
                            )
                        )
                    }
                }

                is AuthResult.Failure -> {

                    _uiState.update {
                        it.copy(isLoading = false)
                    }

                    _events.emit(
                        LoginUiEvent.ShowMessage(result.message)
                    )
                }
            }
        }
    }

    fun onIdentityChanged() {
        _uiState.update {
            it.copy(identityError = null)
        }
    }

    fun onPasswordChanged() {
        _uiState.update {
            it.copy(passwordError = null)
        }
    }
}
