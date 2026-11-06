package com.ecosphere.wastepickup.booking.repository
import com.ecosphere.wastepickup.booking.local.BookingDao
import com.ecosphere.wastepickup.booking.local.BookingEntity
import com.ecosphere.wastepickup.booking.remote.BookingApiService
import com.ecosphere.wastepickup.booking.remote.dto.BookingResponse
import com.ecosphere.wastepickup.booking.remote.dto.CreateBookingRequest
import com.ecosphere.wastepickup.core.security.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

data class BookingSaveResult(
    val booking: BookingEntity,
    val synced: Boolean,
    val message: String
)

class BookingRepository(
    private val api: BookingApiService,
    private val dao: BookingDao,
    private val tokenManager: TokenManager
) {

    fun observeBookings(): Flow<List<BookingEntity>> {
        val ownerId = tokenManager.getUserId() ?: return flowOf(emptyList())
        return dao.getAllBookings(ownerId)
    }

    fun observeBooking(id: String): Flow<BookingEntity?> {
        val ownerId = tokenManager.getUserId() ?: return flowOf(null)
        return dao.getBookingById(id, ownerId)
    }

    suspend fun createBooking(
        request: CreateBookingRequest
    ): BookingSaveResult {

        val ownerId = tokenManager.getUserId().orEmpty()
        val localBooking = BookingEntity(
            id = request.id,
            ownerId = ownerId,
            customerName = request.customerName,
            mobile = request.mobile,
            category = request.category,
            address = request.address,
            pickupDate = request.pickupDate,
            pickupTime = request.pickupTime,
            status = "Pending",
            isSynced = false,
            createdAt = System.currentTimeMillis().toString()
        )

        // Persist locally before the API call.
        dao.upsert(localBooking)

        val token = tokenManager.getToken()
            ?: return BookingSaveResult(
                localBooking,
                false,
                "Saved locally. Please log in to sync."
            )

        return try {

            val response = api.createBooking(
                "Bearer $token",
                request
            )

            val serverBooking = response.body()

            if (response.isSuccessful && serverBooking != null) {

                val syncedBooking = serverBooking.toEntity(ownerId, true)
                    ?: return BookingSaveResult(localBooking, false, "Saved locally. The server returned incomplete booking details.")

                dao.upsert(syncedBooking)

                BookingSaveResult(
                    syncedBooking,
                    true,
                    "Booking created successfully."
                )

            } else {

                BookingSaveResult(
                    localBooking,
                    false,
                    "Saved locally, but server sync failed."
                )
            }

        } catch (e: Exception) {

            BookingSaveResult(
                localBooking,
                false,
                "Saved locally. Unable to reach the server."
            )
        }
    }

    suspend fun refreshBookings(): String {

        val token = tokenManager.getToken()
            ?: return "Please log in to sync bookings."
        val ownerId = tokenManager.getUserId()
            ?: return "Please log in to sync bookings."

        return try {

            val response = api.getBookings("Bearer $token")

            if (response.isSuccessful) {

                val remoteBookings = response.body().orEmpty()

                dao.upsertAll(
                    remoteBookings.mapNotNull { it.toEntity(ownerId, true) }
                )

                retryPendingBookings(token, ownerId)

                "Bookings synchronized."

            } else {
                errorMessage(response.errorBody()?.string()) ?: "Unable to refresh bookings."
            }

        } catch (e: Exception) {
            "Offline. Showing locally saved bookings."
        }
    }

    suspend fun refreshBooking(id: String) {
        val token = tokenManager.getToken() ?: throw IOException("Please log in again.")
        val ownerId = tokenManager.getUserId() ?: throw IOException("Please log in again.")
        val response = api.getBooking("Bearer $token", id)
        if (!response.isSuccessful) {
            throw IOException("Unable to refresh booking (${response.code()}).")
        }
        val booking = response.body()?.toEntity(ownerId, true)
            ?: throw IOException("The server returned incomplete booking details.")
        dao.upsert(booking)
    }

    private suspend fun retryPendingBookings(token: String, ownerId: String) {
        dao.getUnsyncedBookings(ownerId).forEach { local ->
            try {
                val response = api.createBooking("Bearer $token", local.toCreateRequest())
                val remote = response.body()?.toEntity(ownerId, true)
                if (response.isSuccessful && remote != null) dao.upsert(remote)
            } catch (_: Exception) {
                // Keep the local booking for a later refresh attempt.
            }
        }
    }

    suspend fun cancelBooking(id: String): BookingEntity {

        val token = tokenManager.getToken()
            ?: throw IOException("Please log in again.")
        val ownerId = tokenManager.getUserId()
            ?: throw IOException("Please log in again.")
        val existing = dao.findBooking(id, ownerId)
            ?: throw IOException("Booking not found.")

        if (existing.status !in listOf("Pending", "Confirmed")) {
            throw IOException("Only pending or confirmed bookings can be cancelled.")
        }

        val updatedBooking = if (existing.isSynced) {

            val response = api.cancelBooking(
                "Bearer $token",
                id
            )

            if (!response.isSuccessful) {
                throw IOException(errorMessage(response.errorBody()?.string()) ?: "Unable to cancel booking. Please try again.")
            }

            val serverBooking = response.body()
                ?: throw IOException("Empty cancellation response.")

            serverBooking.toEntity(ownerId, true)
                ?: throw IOException("The server returned incomplete booking details. Please try again.")

        } else {

            // Unsynced booking can be cancelled locally.
            existing.copy(status = "Cancelled")
        }

        dao.upsert(updatedBooking)

        return updatedBooking
    }
}

private fun BookingResponse.toEntity(
    ownerId: String,
    synced: Boolean
) : BookingEntity? {
    val bookingId = id ?: return null
    val name = customerName ?: return null
    val phone = mobile ?: return null
    val wasteCategory = category ?: return null
    val pickupAddress = address ?: return null
    val date = pickupDate ?: return null
    val time = pickupTime ?: return null
    val bookingStatus = status ?: return null
    return BookingEntity(
    id = bookingId,
    ownerId = ownerId,
    customerName = name,
    mobile = phone,
    category = wasteCategory,
    address = pickupAddress,
    pickupDate = date,
    pickupTime = time,
    status = bookingStatus,
    isSynced = synced,
    createdAt = createdAt.orEmpty()
)}

private fun BookingEntity.toCreateRequest() = CreateBookingRequest(
    id = id, customerName = customerName, mobile = mobile, category = category,
    address = address, pickupDate = pickupDate, pickupTime = pickupTime
)

private fun BookingRepository.errorMessage(body: String?): String? = try {
    com.google.gson.Gson().fromJson(body, ApiError::class.java)?.message?.takeIf { it.isNotBlank() }
} catch (_: Exception) { null }

private data class ApiError(val message: String?)
