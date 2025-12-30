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
    val lastSyncAttempt: Long = 0,

    // Extended metrics (added in v7)
    /** Average power in watts */
    val powerAvg: Int = 0,

    /** Maximum power in watts */
    val powerMax: Int = 0,

    /** Average cadence in RPM */
    val cadenceAvg: Int = 0,

    /** Average heart rate in BPM */
    val heartRateAvg: Int = 0,

    /** Maximum heart rate in BPM */
    val heartRateMax: Int = 0,

    /** Average speed in km/h */
    val speedAvgKmh: Float = 0f,

    /** Total distance in km */
    val distanceKm: Float = 0f,

    // Pro cyclist metrics (added in v8)
    /** Total elevation gained in meters */
    val elevationGain: Int = 0,

    /** Total elevation lost in meters */
    val elevationLoss: Int = 0,

    /** Average gradient percentage */
    val gradeAvg: Float = 0f,

    /** Maximum gradient percentage */
    val gradeMax: Float = 0f,

    /** Normalized Power in watts */
    val normalizedPower: Int = 0,

    /** Energy output in kilojoules */
    val energyKj: Int = 0,

    // Score (added in v9 - unified formula)
    /** Overall ride score (0-100), calculated using unified formula */
    val score: Int = 0
) {
    companion object {
        const val SYNC_STATUS_PENDING = 0
        const val SYNC_STATUS_SYNCED = 1
        const val SYNC_STATUS_FAILED = 2
    }
}
