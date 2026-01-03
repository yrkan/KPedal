package io.github.kpedal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table for storing in-progress ride checkpoint.
 * Used to recover ride data after crash or app restart.
 *
 * Only one checkpoint can exist at a time (PrimaryKey = 1).
 * Checkpoint is cleared when ride ends normally.
 */
@Entity(tableName = "ride_checkpoint")
data class RideCheckpointEntity(
    @PrimaryKey
    val id: Int = 1,  // Single row table

    /** Timestamp when ride started (ms) */
    val rideStartTimeMs: Long,

    /** Timestamp of this checkpoint (ms) */
    val lastCheckpointMs: Long,

    /** Number of samples collected */
    val sampleCount: Int,

    /** JSON-serialized AccumulatorState containing all accumulator values */
    val accumulatorsJson: String,

    /** JSON-serialized List<RideSnapshot> for per-minute data */
    val snapshotsJson: String,

    /** Whether ride was actively recording when checkpoint was saved */
    val wasRecording: Boolean
)
