package com.ecosphere.wastepickup.core.di

import android.content.Context
import androidx.room.Room
import com.ecosphere.wastepickup.auth.repository.AuthRepository
import com.ecosphere.wastepickup.booking.repository.BookingRepository
import com.ecosphere.wastepickup.core.database.AppDatabase
import com.ecosphere.wastepickup.core.database.MIGRATION_1_2
import com.ecosphere.wastepickup.core.network.ApiClient
import com.ecosphere.wastepickup.core.security.TokenManager

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val tokenManager: TokenManager by lazy {
        TokenManager(appContext)
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "ecosphere_database"
        ).addMigrations(MIGRATION_1_2).build()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(ApiClient.authApiService)
    }

    val bookingRepository: BookingRepository by lazy {
        BookingRepository(
            ApiClient.bookingApiService,
            database.bookingDao(),
            tokenManager
        )
    }
}
