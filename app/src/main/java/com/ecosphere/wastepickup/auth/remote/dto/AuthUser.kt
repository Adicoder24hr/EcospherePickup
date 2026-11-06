package com.ecosphere.wastepickup.auth.remote.dto

data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val mobile: String
)