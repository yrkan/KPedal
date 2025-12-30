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
    version = 10,
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
         * Migration from v6 to v7: Add extended metrics to rides table.
         * Adds power, cadence, heart rate, speed, and distance columns.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rides ADD COLUMN powerAvg INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN powerMax INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN cadenceAvg INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN heartRateAvg INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN speedAvgKmh REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN distanceKm REAL NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from v7 to v8: Add sync status to drill_results and achievements.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add sync status to drill_results
                db.execSQL("ALTER TABLE drill_results ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE drill_results ADD COLUMN lastSyncAttempt INTEGER NOT NULL DEFAULT 0")
                // Add sync status to achievements
                db.execSQL("ALTER TABLE achievements ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE achievements ADD COLUMN lastSyncAttempt INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from v8 to v9: Add pro cyclist metrics to rides table.
         * Adds elevation, grade, normalized power, energy columns.
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add max heart rate
                db.execSQL("ALTER TABLE rides ADD COLUMN heartRateMax INTEGER NOT NULL DEFAULT 0")
                // Add climbing metrics
                db.execSQL("ALTER TABLE rides ADD COLUMN elevationGain INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN elevationLoss INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN gradeAvg REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN gradeMax REAL NOT NULL DEFAULT 0")
                // Add power analytics
                db.execSQL("ALTER TABLE rides ADD COLUMN normalizedPower INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN energyKj INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from v9 to v10: Add unified score to rides table.
         * Score is calculated using unified formula: 40% balance + 35% efficiency + 25% consistency.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rides ADD COLUMN score INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATIONS = arrayOf<Migration>(
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10
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
