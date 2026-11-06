
package com.ecosphere.wastepickup.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecosphere.wastepickup.auth.remote.dto.RegisterRequest
import com.ecosphere.wastepickup.auth.repository.AuthRepository
import com.ecosphere.wastepickup.auth.repository.AuthResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val nameError: String? = null,
    val emailError: String? = null,
    val mobileError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false
)

sealed interface RegisterUiEvent {
    data class Success(val message: String) : RegisterUiEvent
    data class ShowMessage(val message: String) : RegisterUiEvent
}

class RegisterViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val _events =
        MutableSharedFlow<RegisterUiEvent>(extraBufferCapacity = 1)

    val events = _events.asSharedFlow()

    fun register(
        name: String,
        email: String,
        mobile: String,
        password: String,
        confirmPassword: String
    ) {
        if (_uiState.value.isLoading) return

        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanMobile = mobile.trim()

        val emailRegex =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        val mobileRegex =
            Regex("^\\+?[0-9]{10,15}$")

        val nameError = when {
            cleanName.isEmpty() ->
                "Full name is required"

            cleanName.length < 2 ->
                "Enter a valid name"

            else -> null
        }

        val emailError = when {
            cleanEmail.isEmpty() ->
                "Email is required"

            !emailRegex.matches(cleanEmail) ->
                "Enter a valid email address"

            else -> null
        }

        val mobileError = when {
            cleanMobile.isEmpty() ->
                "Mobile number is required"

            !mobileRegex.matches(cleanMobile) ->
                "Enter a valid mobile number"

            else -> null
        }

        val passwordError = when {
            password.isEmpty() ->
                "Password is required"

            password.length < 8 ->
                "Password must contain at least 8 characters"

            else -> null
        }

        val confirmPasswordError = when {
            confirmPassword.isEmpty() ->
                "Please confirm your password"

            password != confirmPassword ->
                "Passwords do not match"

            else -> null
        }

        _uiState.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                mobileError = mobileError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError
            )
        }

        val isValid = listOf(
            nameError,
            emailError,
            mobileError,
            passwordError,
            confirmPasswordError
        ).all { it == null }

        if (!isValid) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(isLoading = true)
            }

            val request = RegisterRequest(
                name = cleanName,
                email = cleanEmail,
                mobile = cleanMobile,
                password = password
            )

            when (val result = repository.register(request)) {

                is AuthResult.Success -> {

                    _uiState.update {
                        it.copy(isLoading = false)
                    }

                    _events.emit(
                        RegisterUiEvent.Success(
                            result.data.message
                        )
                    )
                }

                is AuthResult.Failure -> {

                    _uiState.update {
                        it.copy(isLoading = false)
                    }

                    _events.emit(
                        RegisterUiEvent.ShowMessage(result.message)
                    )
                }
            }
        }
    }

    fun onNameChanged() {
        _uiState.update { it.copy(nameError = null) }
    }

    fun onEmailChanged() {
        _uiState.update { it.copy(emailError = null) }
    }

    fun onMobileChanged() {
        _uiState.update { it.copy(mobileError = null) }
    }

    fun onPasswordChanged() {
        _uiState.update {
            it.copy(
                passwordError = null,
                confirmPasswordError = null
            )
        }
    }

    fun onConfirmPasswordChanged() {
        _uiState.update {
            it.copy(confirmPasswordError = null)
        }
    }
}
