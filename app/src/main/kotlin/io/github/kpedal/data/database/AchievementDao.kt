package io.github.kpedal.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for achievement operations.
 */
@Dao
interface AchievementDao {

    /**
     * Get all unlocked achievements.
     */
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun getAllAchievementsFlow(): Flow<List<AchievementEntity>>

    /**
     * Get all unlocked achievements (one-time).
     */
    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<AchievementEntity>

    /**
     * Check if an achievement is unlocked.
     */
    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievement(id: String): AchievementEntity?

    /**
     * Get count of unlocked achievements.
     */
    @Query("SELECT COUNT(*) FROM achievements")
    fun getUnlockedCountFlow(): Flow<Int>

    /**
     * Unlock an achievement.
     * Returns row ID if inserted, -1 if already exists (IGNORE).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity): Long

    /**
     * Delete all achievements (for testing/reset).
     */
    @Query("DELETE FROM achievements")
    suspend fun deleteAll()

    // Sync methods

    /**
     * Get all pending sync achievements.
     */
    @Query("SELECT * FROM achievements WHERE syncStatus = 0 ORDER BY unlockedAt ASC")
    suspend fun getPendingSync(): List<AchievementEntity>

    /**
     * Get count of pending sync achievements.
     */
    @Query("SELECT COUNT(*) FROM achievements WHERE syncStatus = 0")
    fun getPendingSyncCountFlow(): Flow<Int>

    /**
     * Mark an achievement as synced.
     */
    @Query("UPDATE achievements SET syncStatus = 1, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark an achievement as sync failed.
     */
    @Query("UPDATE achievements SET syncStatus = 2, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun markAsSyncFailed(id: String, timestamp: Long = System.currentTimeMillis())
}
