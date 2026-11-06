package com.ecosphere.wastepickup.booking.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {

    @Upsert
    suspend fun upsert(booking: BookingEntity)

    @Upsert
    suspend fun upsertAll(bookings: List<BookingEntity>)

    @Query("""
    SELECT * FROM bookings
    WHERE ownerId = :ownerId
    ORDER BY pickupDate DESC
""")
    fun getAllBookings(ownerId: String): Flow<List<BookingEntity>>

    @Query("""
        SELECT * FROM bookings
        WHERE id = :bookingId AND ownerId = :ownerId
        LIMIT 1
    """)
    fun getBookingById(
        bookingId: String,
        ownerId: String
    ): Flow<BookingEntity?>

    @Query("""
        SELECT * FROM bookings
        WHERE id = :bookingId AND ownerId = :ownerId
        LIMIT 1
    """)
    suspend fun findBooking(
        bookingId: String,
        ownerId: String
    ): BookingEntity?

    @Query("SELECT * FROM bookings WHERE ownerId = :ownerId AND isSynced = 0 AND status = 'Pending'")
    suspend fun getUnsyncedBookings(ownerId: String): List<BookingEntity>

    @Query("SELECT COUNT(*) FROM bookings")
    fun getTotalBookings(): Flow<Int>
}
