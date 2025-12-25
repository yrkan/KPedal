package io.github.kpedal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for kpedal.
 * Stores ride history, drill results, and achievements.
 *
 * Migration strategy:
 * - Versions 1-5: Used destructive migration (legacy)
 * - Version 6+: Proper migrations to preserve user data
 */
@Database(
    entities = [RideEntity::class, DrillResultEntity::class, AchievementEntity::class, CustomDrillEntity::class],
    version = 6,
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

        /**
         * Migrations for future schema changes.
         * Add new migrations here as: MIGRATION_X_Y
         *
         * Example for version 6 to 7:
         * val MIGRATION_6_7 = object : Migration(6, 7) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE rides ADD COLUMN newField TEXT DEFAULT ''")
         *     }
         * }
         */
        private val MIGRATIONS = arrayOf<Migration>(
            // Add migrations here as needed
            // MIGRATION_6_7,
        )

        fun getInstance(context: Context): KPedalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KPedalDatabase::class.java,
                    DATABASE_NAME
                )
                    // Only allow destructive migration from versions before 6
                    // This preserves data for users upgrading from v6+
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
