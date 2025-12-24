package io.github.kpedal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a saved ride in the database.
 */
@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Timestamp when ride ended (System.currentTimeMillis()) */
    val timestamp: Long,

    /** Duration in milliseconds for accurate storage */
    val durationMs: Long,

    /** Formatted duration string for display ("H:MM:SS") */
    val durationFormatted: String,

    /** Average left balance percentage */
    val balanceLeft: Int,

    /** Average right balance percentage */
    val balanceRight: Int,

    /** Average left torque effectiveness percentage */
    val teLeft: Int,

    /** Average right torque effectiveness percentage */
    val teRight: Int,

    /** Average left pedal smoothness percentage */
    val psLeft: Int,

    /** Average right pedal smoothness percentage */
    val psRight: Int,

    /** Percentage of time in optimal zone */
    val zoneOptimal: Int,

    /** Percentage of time in attention zone */
    val zoneAttention: Int,

    /** Percentage of time in problem zone */
    val zoneProblem: Int,

    /** True if saved via manual button, false if auto-saved on ride end */
    val savedManually: Boolean = false,

    /** User rating 1-5 stars (0 = unrated) */
    val rating: Int = 0,

    /** User notes about the ride */
    val notes: String = "",

    /** Sync status: 0=pending, 1=synced, 2=failed */
    val syncStatus: Int = SYNC_STATUS_PENDING,

    /** Last sync attempt timestamp (0 = never attempted) */
    val lastSyncAttempt: Long = 0
) {
    companion object {
        const val SYNC_STATUS_PENDING = 0
        const val SYNC_STATUS_SYNCED = 1
        const val SYNC_STATUS_FAILED = 2
    }
}
