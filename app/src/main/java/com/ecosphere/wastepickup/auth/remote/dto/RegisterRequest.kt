package com.ecosphere.wastepickup.auth.remote.dto

data class RegisterRequest(
    val name: String,
    val email: String,
    val mobile: String,
    val password: String
)