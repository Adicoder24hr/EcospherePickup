package com.ecosphere.wastepickup.auth.repository

import com.ecosphere.wastepickup.auth.remote.AuthApiService
import com.ecosphere.wastepickup.auth.remote.dto.AuthResponse
import com.ecosphere.wastepickup.auth.remote.dto.RegisterRequest
import com.ecosphere.wastepickup.auth.remote.dto.loginRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

sealed interface AuthResult {

    data class Success(
        val data: AuthResponse
    ) : AuthResult

    data class Failure(
        val message: String
    ) : AuthResult
}

private data class ApiErrorResponse(
    val message: String?
)

class AuthRepository(
    private val apiService: AuthApiService
) {

    private val gson = Gson()

    suspend fun register(
        request: RegisterRequest
    ): AuthResult = execute {
        apiService.register(request)
    }

    suspend fun login(
        request: loginRequest
    ): AuthResult = execute {
        apiService.login(request)
    }

    private suspend fun execute(
        request: suspend () -> Response<AuthResponse>
    ): AuthResult = withContext(Dispatchers.IO) {

        try {
            val response = request()

            if (response.isSuccessful) {

                val body = response.body()

                if (body != null) {
                    AuthResult.Success(body)
                } else {
                    AuthResult.Failure(
                        "The server returned an empty response."
                    )
                }

            } else {

                val errorMessage = try {
                    val errorJson =
                        response.errorBody()?.string()

                    gson.fromJson(
                        errorJson,
                        ApiErrorResponse::class.java
                    )?.message

                } catch (e: Exception) {
                    null
                }

                AuthResult.Failure(
                    errorMessage
                        ?: "Request failed: ${response.code()}"
                )
            }

        } catch (e: IOException) {

            AuthResult.Failure(
                "Unable to connect to server. Check your internet connection."
            )

        } catch (e: Exception) {

            AuthResult.Failure(
                e.message ?: "Something went wrong. Please try again."
            )
        }
    }
}