package io.github.kpedal.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for drill results.
 */
@Dao
interface DrillResultDao {

    /**
     * Insert a drill result.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: DrillResultEntity): Long

    /**
     * Get all drill results ordered by timestamp (newest first).
     */
    @Query("SELECT * FROM drill_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<DrillResultEntity>>

    /**
     * Get results for a specific drill.
     */
    @Query("SELECT * FROM drill_results WHERE drillId = :drillId ORDER BY timestamp DESC")
    fun getResultsForDrill(drillId: String): Flow<List<DrillResultEntity>>

    /**
     * Get a single result by ID.
     */
    @Query("SELECT * FROM drill_results WHERE id = :id")
    suspend fun getResult(id: Long): DrillResultEntity?

    /**
     * Get best score for a drill (completed only).
     */
    @Query("SELECT MAX(score) FROM drill_results WHERE drillId = :drillId AND completed = 1")
    suspend fun getBestScore(drillId: String): Float?

    /**
     * Get count of completed drills for a specific drill type.
     */
    @Query("SELECT COUNT(*) FROM drill_results WHERE drillId = :drillId AND completed = 1")
    suspend fun getCompletedCount(drillId: String): Int

    /**
     * Get total count of all completed drills (across all drill types).
     */
    @Query("SELECT COUNT(*) FROM drill_results WHERE completed = 1")
    suspend fun getAllCompletedCount(): Int

    /**
     * Delete a result.
     */
    @Query("DELETE FROM drill_results WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Delete all results for a drill.
     */
    @Query("DELETE FROM drill_results WHERE drillId = :drillId")
    suspend fun deleteForDrill(drillId: String)

    /**
     * Delete all drill results.
     */
    @Query("DELETE FROM drill_results")
    suspend fun deleteAll()

    /**
     * Keep only last 50 results per drill to prevent bloat.
     */
    @Query("""
        DELETE FROM drill_results WHERE id IN (
            SELECT id FROM drill_results WHERE drillId = :drillId
            ORDER BY timestamp DESC LIMIT -1 OFFSET 50
        )
    """)
    suspend fun trimResultsForDrill(drillId: String)

    // Sync methods

    /**
     * Get all pending sync drill results.
     */
    @Query("SELECT * FROM drill_results WHERE syncStatus = 0 ORDER BY timestamp ASC")
    suspend fun getPendingSync(): List<DrillResultEntity>

    /**
     * Get count of pending sync drill results.
     */
    @Query("SELECT COUNT(*) FROM drill_results WHERE syncStatus = 0")
    fun getPendingSyncCountFlow(): Flow<Int>

    /**
     * Mark a drill result as synced.
     */
    @Query("UPDATE drill_results SET syncStatus = 1, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark a drill result as sync failed.
     */
    @Query("UPDATE drill_results SET syncStatus = 2, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun markAsSyncFailed(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Get all drill results that need sync (pending or failed).
     * Failed results are retried on each sync attempt.
     */
    @Query("SELECT * FROM drill_results WHERE syncStatus IN (0, 2) ORDER BY timestamp ASC")
    suspend fun getDrillsNeedingSync(): List<DrillResultEntity>

    /**
     * Get count of failed sync drill results (reactive).
     */
    @Query("SELECT COUNT(*) FROM drill_results WHERE syncStatus = 2")
    fun getFailedSyncCountFlow(): Flow<Int>

    /**
     * Get total count of drill results needing sync (pending + failed).
     */
    @Query("SELECT COUNT(*) FROM drill_results WHERE syncStatus IN (0, 2)")
    fun getNeedingSyncCountFlow(): Flow<Int>

    /**
     * Reset failed drill results to pending (for manual retry).
     */
    @Query("UPDATE drill_results SET syncStatus = 0 WHERE syncStatus = 2")
    suspend fun resetFailedToPending()
}
