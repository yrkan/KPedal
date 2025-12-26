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
    val progress: Int = 100,
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
