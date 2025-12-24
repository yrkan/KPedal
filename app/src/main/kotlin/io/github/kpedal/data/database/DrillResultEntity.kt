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
    val phaseScoresJson: String  // JSON array of phase scores
)
