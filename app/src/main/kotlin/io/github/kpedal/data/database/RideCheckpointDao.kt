package io.github.kpedal.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for ride checkpoint operations.
 * Single-row table - only one checkpoint at a time.
 */
@Dao
interface RideCheckpointDao {

    /**
     * Get the current checkpoint if it exists.
     */
    @Query("SELECT * FROM ride_checkpoint WHERE id = 1")
    suspend fun getCheckpoint(): RideCheckpointEntity?

    /**
     * Save or update the checkpoint.
     * Uses REPLACE strategy since there's only one row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCheckpoint(checkpoint: RideCheckpointEntity)

    /**
     * Clear the checkpoint.
     * Called when ride ends normally.
     */
    @Query("DELETE FROM ride_checkpoint")
    suspend fun clearCheckpoint()
}
