
package com.ecosphere.wastepickup.core.network

import com.ecosphere.wastepickup.auth.remote.AuthApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.ecosphere.wastepickup.booking.remote.BookingApiService

object ApiClient {
    private const val BASE_URL =
        "https://ecosphere-backend-7tlm.onrender.com/api/"

    val bookingApiService: BookingApiService by lazy {
        retrofit.create(BookingApiService::class.java)
    }

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            // BASIC logs request URLs and response codes,
            // but does not log passwords or response bodies.
            level = HttpLoggingInterceptor.Level.BASIC
        }

    private val okHttpClient: OkHttpClient by lazy {

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}
