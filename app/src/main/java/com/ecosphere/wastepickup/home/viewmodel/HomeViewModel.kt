package com.ecosphere.wastepickup.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecosphere.wastepickup.booking.local.BookingEntity
import com.ecosphere.wastepickup.booking.repository.BookingRepository
import com.ecosphere.wastepickup.core.security.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val customerName: String = "Customer",
    val email: String = "",
    val totalBookings: Int = 0,
    val pendingBookings: Int = 0,
    val completedBookings: Int = 0,
    val recentBookings: List<BookingEntity> = emptyList(),
    val isRefreshing: Boolean = false
)

class HomeViewModel(
    private val repository: BookingRepository,
    tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            customerName = tokenManager.getUserName(),
            email = tokenManager.getUserEmail()
        )
    )

    val uiState = _uiState.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {

            repository.observeBookings().collect { bookings ->

                _uiState.value = _uiState.value.copy(
                    totalBookings = bookings.size,

                    pendingBookings = bookings.count {
                        it.status == "Pending"
                    },

                    completedBookings = bookings.count {
                        it.status == "Completed"
                    },

                    recentBookings = bookings.sortedByDescending { it.createdAt }.take(3)
                )
            }
        }
    }

    fun refreshBookings() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val message = repository.refreshBookings()
                if (message != "Bookings synchronized.") _messages.emit(message)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }
}
