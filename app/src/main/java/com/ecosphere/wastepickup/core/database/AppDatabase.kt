package com.ecosphere.wastepickup.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ecosphere.wastepickup.booking.local.BookingDao
import com.ecosphere.wastepickup.booking.local.BookingEntity

@Database(
    entities = [BookingEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookingDao(): BookingDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE bookings ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''"
        )
    }
}
