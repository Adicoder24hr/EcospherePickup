package com.ecosphere.wastepickup.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ecosphere.wastepickup.auth.repository.AuthRepository
import com.ecosphere.wastepickup.auth.viewmodel.LoginViewModel
import com.ecosphere.wastepickup.auth.viewmodel.RegisterViewModel
import com.ecosphere.wastepickup.core.security.TokenManager

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        return when {

            modelClass.isAssignableFrom(
                LoginViewModel::class.java
            ) -> LoginViewModel(
                authRepository,
                tokenManager
            ) as T

            modelClass.isAssignableFrom(
                RegisterViewModel::class.java
            ) -> RegisterViewModel(
                authRepository
            ) as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}