package io.github.kpedal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing unlocked achievements.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
    val progress: Int = 100
)
