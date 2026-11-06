package com.ecosphere.wastepickup.booking.remote

import com.ecosphere.wastepickup.booking.remote.dto.BookingResponse
import com.ecosphere.wastepickup.booking.remote.dto.CreateBookingRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path

interface BookingApiService {

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body request: CreateBookingRequest
    ): Response<BookingResponse>

    @GET("bookings")
    suspend fun getBookings(
        @Header("Authorization") token: String
    ): Response<List<BookingResponse>>

    @GET("bookings/{id}")
    suspend fun getBooking(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<BookingResponse>

    @PATCH("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<BookingResponse>
}