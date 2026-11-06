
package com.ecosphere.wastepickup.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecosphere.wastepickup.booking.local.BookingEntity
import com.ecosphere.wastepickup.booking.remote.dto.CreateBookingRequest
import com.ecosphere.wastepickup.booking.repository.BookingRepository
import com.ecosphere.wastepickup.booking.util.BookingReferenceFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BookingUiState(
    val nameError: String? = null,
    val mobileError: String? = null,
    val categoryError: String? = null,
    val addressError: String? = null,
    val dateError: String? = null,
    val timeError: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

sealed interface BookingUiEvent {

    data class Created(
        val bookingId: String,
        val message: String
    ) : BookingUiEvent

    data class Message(
        val message: String
    ) : BookingUiEvent

    data object Cancelled : BookingUiEvent
}

class BookingViewModel(
    private val repository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState = _uiState.asStateFlow()

    private val _events =
        MutableSharedFlow<BookingUiEvent>(extraBufferCapacity = 1)

    val events = _events.asSharedFlow()

    val bookings: Flow<List<BookingEntity>> =
        repository.observeBookings()

    fun observeBooking(id: String): Flow<BookingEntity?> =
        repository.observeBooking(id)

    fun refreshBookings() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val message = repository.refreshBookings()
                if (message != "Bookings synchronized.") {
                    _events.emit(BookingUiEvent.Message(message))
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshBooking(id: String) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                repository.refreshBooking(id)
            } catch (_: Exception) {
                _events.emit(BookingUiEvent.Message("Unable to refresh booking. Showing saved details."))
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun createBooking(
        name: String,
        mobile: String,
        category: String,
        address: String,
        date: String,
        time: String
    ) {
        if (_uiState.value.isLoading) return

        val cleanName = name.trim()
        val cleanMobile = mobile.trim()
        val cleanAddress = address.trim()

        val mobileRegex = Regex("^\\+?[0-9]{10,15}$")

        val categories = listOf(
            "Plastic",
            "Paper",
            "E-waste",
            "General Waste"
        )

        val nameError = when {
            cleanName.isEmpty() -> "Name is required"
            cleanName.length < 2 -> "Enter a valid name"
            else -> null
        }

        val mobileError = when {
            cleanMobile.isEmpty() -> "Mobile number is required"
            !mobileRegex.matches(cleanMobile) ->
                "Enter a valid mobile number"
            else -> null
        }

        val categoryError =
            if (category !in categories) "Select a waste category"
            else null

        val addressError = when {
            cleanAddress.isEmpty() -> "Address is required"
            cleanAddress.length < 5 -> "Enter a complete address"
            else -> null
        }

        val dateError =
            if (date.isBlank()) "Select pickup date" else null

        val timeError =
            if (time.isBlank()) "Select pickup time" else null

        _uiState.update {
            it.copy(
                nameError = nameError,
                mobileError = mobileError,
                categoryError = categoryError,
                addressError = addressError,
                dateError = dateError,
                timeError = timeError
            )
        }

        val valid = listOf(
            nameError,
            mobileError,
            categoryError,
            addressError,
            dateError,
            timeError
        ).all { it == null }

        if (!valid) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(isLoading = true)
            }

            try {

                val id = UUID.randomUUID().toString()

                val result = repository.createBooking(
                    CreateBookingRequest(
                        id = id,
                        customerName = cleanName,
                        mobile = cleanMobile,
                        category = category,
                        address = cleanAddress,
                        pickupDate = date,
                        pickupTime = time
                    )
                )

                _uiState.update {
                    it.copy(isLoading = false)
                }

                _events.emit(
                    BookingUiEvent.Created(
                        result.booking.id,
                        "${result.message} Reference: ${BookingReferenceFormatter.format(result.booking.id)}"
                    )
                )

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(isLoading = false)
                }

                _events.emit(
                    BookingUiEvent.Message(
                        e.message ?: "Unable to create booking"
                    )
                )
            }
        }
    }

    fun cancelBooking(id: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                repository.cancelBooking(id)

                _events.emit(
                    BookingUiEvent.Cancelled
                )

            } catch (e: Exception) {

                _events.emit(
                    BookingUiEvent.Message(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "Unable to cancel this booking. Please try again."
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
