package com.ecosphere.wastepickup.auth.remote.dto

data class AuthResponse(
    val message: String,
    val token: String,
    val user: AuthUser
)