package com.ecosphere.wastepickup.auth.remote.dto

data class loginRequest (
    val emailOrMobile: String,
    val password: String
)