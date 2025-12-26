package io.github.kpedal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for persisting drill results.
 */
@Entity(tableName = "drill_results")
data class DrillResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val drillId: String,
    val drillName: String,
    val timestamp: Long,
    val durationMs: Long,
    val score: Float,
    val timeInTargetMs: Long,
    val timeInTargetPercent: Float,
    val completed: Boolean,
    val phaseScoresJson: String,  // JSON array of phase scores
    // Sync status (added in v8)
    val syncStatus: Int = SYNC_STATUS_PENDING,
    val lastSyncAttempt: Long = 0
) {
    companion object {
        const val SYNC_STATUS_PENDING = 0
        const val SYNC_STATUS_SYNCED = 1
        const val SYNC_STATUS_FAILED = 2
    }
}
