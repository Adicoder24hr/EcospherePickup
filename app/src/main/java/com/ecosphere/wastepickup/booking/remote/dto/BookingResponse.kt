package com.ecosphere.wastepickup.booking.remote.dto

data class BookingResponse(
    val id: String? = null,
    val customerName: String? = null,
    val mobile: String? = null,
    val category: String? = null,
    val address: String? = null,
    val pickupDate: String? = null,
    val pickupTime: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)
