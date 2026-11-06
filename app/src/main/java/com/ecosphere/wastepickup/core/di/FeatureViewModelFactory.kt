package com.ecosphere.wastepickup.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ecosphere.wastepickup.booking.repository.BookingRepository
import com.ecosphere.wastepickup.booking.viewmodel.BookingViewModel
import com.ecosphere.wastepickup.core.security.TokenManager
import com.ecosphere.wastepickup.home.viewmodel.HomeViewModel

class FeatureViewModelFactory(
    private val bookingRepository: BookingRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        return when {

            modelClass.isAssignableFrom(
                HomeViewModel::class.java
            ) -> HomeViewModel(
                bookingRepository,
                tokenManager
            ) as T

            modelClass.isAssignableFrom(
                BookingViewModel::class.java
            ) -> BookingViewModel(
                bookingRepository
            ) as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class"
            )
        }
    }
}