package io.github.kpedal.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ride operations.
 */
@Dao
interface RideDao {

    /**
     * Get all rides ordered by timestamp descending (newest first).
     * Returns Flow for reactive UI updates.
     */
    @Query("SELECT * FROM rides ORDER BY timestamp DESC")
    fun getAllRidesFlow(): Flow<List<RideEntity>>

    /**
     * Get all rides (non-reactive, for one-time reads).
     */
    @Query("SELECT * FROM rides ORDER BY timestamp DESC")
    suspend fun getAllRides(): List<RideEntity>

    /**
     * Get a single ride by ID.
     */
    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getRideById(id: Long): RideEntity?

    /**
     * Get ride count.
     */
    @Query("SELECT COUNT(*) FROM rides")
    suspend fun getRideCount(): Int

    /**
     * Insert a new ride.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long

    /**
     * Delete a ride by ID.
     */
    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteRideById(id: Long)

    /**
     * Delete all rides (for testing/reset).
     */
    @Query("DELETE FROM rides")
    suspend fun deleteAllRides()

    /**
     * Delete oldest rides beyond limit.
     * Keeps the newest `limit` rides, deletes the rest.
     */
    @Query("DELETE FROM rides WHERE id NOT IN (SELECT id FROM rides ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun deleteOldestBeyondLimit(limit: Int)

    // ========== Dashboard Queries ==========

    /**
     * Get the most recent ride.
     */
    @Query("SELECT * FROM rides ORDER BY timestamp DESC LIMIT 1")
    fun getLastRideFlow(): Flow<RideEntity?>

    /**
     * Get total ride count (reactive).
     */
    @Query("SELECT COUNT(*) FROM rides")
    fun getTotalRideCountFlow(): Flow<Int>

    /**
     * Get average balance across all rides.
     * Returns null if no rides exist.
     */
    @Query("SELECT AVG(balanceRight) FROM rides")
    fun getAverageBalanceFlow(): Flow<Float?>

    /**
     * Get rides in date range (for analytics).
     */
    @Query("SELECT * FROM rides WHERE timestamp >= :startMs AND timestamp <= :endMs ORDER BY timestamp DESC")
    suspend fun getRidesInRange(startMs: Long, endMs: Long): List<RideEntity>

    /**
     * Get rides after a certain timestamp (for streak calculation).
     */
    @Query("SELECT * FROM rides WHERE timestamp >= :sinceMs ORDER BY timestamp DESC")
    suspend fun getRidesSince(sinceMs: Long): List<RideEntity>

    // ========== Rating Queries ==========

    /**
     * Update ride rating.
     */
    @Query("UPDATE rides SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Int)

    /**
     * Update ride notes.
     */
    @Query("UPDATE rides SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    // ========== Sync Queries ==========

    /**
     * Get all pending rides (not yet synced).
     */
    @Query("SELECT * FROM rides WHERE syncStatus = 0 ORDER BY timestamp ASC")
    suspend fun getPendingRides(): List<RideEntity>

    /**
     * Get count of pending rides (reactive).
     */
    @Query("SELECT COUNT(*) FROM rides WHERE syncStatus = 0")
    fun getPendingSyncCountFlow(): Flow<Int>

    /**
     * Update sync status for a ride.
     */
    @Query("UPDATE rides SET syncStatus = :status, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: Int, timestamp: Long)

    /**
     * Mark ride as synced.
     */
    @Query("UPDATE rides SET syncStatus = 1, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark ride as sync failed.
     */
    @Query("UPDATE rides SET syncStatus = 2, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun markAsSyncFailed(id: Long, timestamp: Long = System.currentTimeMillis())
}
