package com.ecosphere.wastepickup.auth.remote

import com.ecosphere.wastepickup.auth.remote.dto.AuthResponse
import com.ecosphere.wastepickup.auth.remote.dto.RegisterRequest
import com.ecosphere.wastepickup.auth.remote.dto.loginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: loginRequest
    ): Response<AuthResponse>
}