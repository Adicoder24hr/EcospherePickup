package com.ecosphere.wastepickup.booking.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(

    @PrimaryKey
    val id: String,

    val ownerId: String = "",

    val customerName: String,
    val mobile: String,
    val category: String,
    val address: String,
    val pickupDate: String,
    val pickupTime: String,
    val status: String,

    // False means the record exists locally
    // but hasn't synced successfully with the API.
    val isSynced: Boolean = true,

    val createdAt: String = ""
)
