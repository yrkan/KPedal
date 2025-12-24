package io.github.kpedal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for kpedal.
 * Stores ride history, drill results, and achievements.
 */
@Database(
    entities = [RideEntity::class, DrillResultEntity::class, AchievementEntity::class, CustomDrillEntity::class],
    version = 5,
    exportSchema = true
)
abstract class KPedalDatabase : RoomDatabase() {

    abstract fun rideDao(): RideDao
    abstract fun drillResultDao(): DrillResultDao
    abstract fun achievementDao(): AchievementDao
    abstract fun customDrillDao(): CustomDrillDao

    companion object {
        private const val DATABASE_NAME = "kpedal_rides.db"

        @Volatile
        private var INSTANCE: KPedalDatabase? = null

        fun getInstance(context: Context): KPedalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KPedalDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
