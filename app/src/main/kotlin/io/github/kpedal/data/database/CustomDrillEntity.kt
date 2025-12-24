package io.github.kpedal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user-created custom drills.
 */
@Entity(tableName = "custom_drills")
data class CustomDrillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val metric: String,           // BALANCE, TE, PS
    val durationSeconds: Int,     // Total drill duration
    val targetType: String,       // MIN, MAX, RANGE, EXACT
    val targetMin: Float?,        // For MIN or RANGE
    val targetMax: Float?,        // For MAX or RANGE
    val targetValue: Float?,      // For EXACT (with tolerance)
    val tolerance: Float = 2f,    // Tolerance for EXACT target
    val createdAt: Long = System.currentTimeMillis()
)
