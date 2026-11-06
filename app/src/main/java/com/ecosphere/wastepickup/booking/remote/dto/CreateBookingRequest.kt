package com.ecosphere.wastepickup.booking.remote.dto

data class CreateBookingRequest(
    val id: String,
    val customerName: String,
    val mobile: String,
    val category: String,
    val address: String,
    val pickupDate: String,
    val pickupTime: String
)