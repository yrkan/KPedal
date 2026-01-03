package io.github.kpedal.engine.checkpoint

import io.github.kpedal.data.database.RideCheckpointDao
import io.github.kpedal.data.database.RideCheckpointEntity
import io.github.kpedal.engine.LiveDataCollector
import io.github.kpedal.engine.RideSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages ride checkpoint persistence for crash recovery.
 *
 * Features:
 * - Periodic checkpoints every 1 minute during recording
 * - Immediate checkpoint on pause
 * - Emergency checkpoint on service destroy
 * - Silent restore on app launch
 */
class RideCheckpointManager(
    private val checkpointDao: RideCheckpointDao,
    private val liveDataCollector: LiveDataCollector
) {
    companion object {
        private const val TAG = "RideCheckpointManager"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastCheckpointMs: Long = 0L
    private var checkpointJob: Job? = null

    /**
     * Start periodic checkpoint saving.
     * Called when ride recording starts.
     */
    fun startPeriodicCheckpoints() {
        checkpointJob?.cancel()
        lastCheckpointMs = System.currentTimeMillis()

        checkpointJob = scope.launch {
            android.util.Log.i(TAG, "Started periodic checkpoints")
            while (isActive) {
                delay(CheckpointDecider.CHECKPOINT_INTERVAL_MS)
                saveCheckpointIfNeeded()
            }
        }
    }

    /**
     * Stop periodic checkpoint saving.
     * Called when ride recording stops.
     */
    fun stopPeriodicCheckpoints() {
        checkpointJob?.cancel()
        checkpointJob = null
        android.util.Log.i(TAG, "Stopped periodic checkpoints")
    }

    /**
     * Save checkpoint if conditions are met.
     * Used by periodic job.
     */
    private suspend fun saveCheckpointIfNeeded() {
        val now = System.currentTimeMillis()
        val sampleCount = liveDataCollector.getSampleCount()

        val decision = CheckpointDecider.shouldSaveCheckpoint(
            lastCheckpointMs = lastCheckpointMs,
            currentTimeMs = now,
            sampleCount = sampleCount,
            isRecording = true
        )

        if (decision.shouldSave) {
            saveCheckpoint(wasRecording = true)
            lastCheckpointMs = now
            android.util.Log.d(TAG, "Checkpoint saved: ${decision.reason}, samples=$sampleCount")
        }
    }

    /**
     * Save checkpoint immediately.
     * Called on pause, emergency, or periodic save.
     *
     * @param wasRecording Whether ride was actively recording
     */
    suspend fun saveCheckpoint(wasRecording: Boolean) {
        try {
            val state = liveDataCollector.createCheckpointState()
            val snapshots = liveDataCollector.getSnapshots()

            val entity = RideCheckpointEntity(
                rideStartTimeMs = state.startTimeMs,
                lastCheckpointMs = System.currentTimeMillis(),
                sampleCount = state.sampleCount,
                accumulatorsJson = json.encodeToString(state),
                snapshotsJson = json.encodeToString(snapshots),
                wasRecording = wasRecording
            )

            checkpointDao.saveCheckpoint(entity)
            android.util.Log.i(TAG, "Saved checkpoint: samples=${state.sampleCount}, snapshots=${snapshots.size}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save checkpoint: ${e.message}")
        }
    }

    /**
     * Try to restore from checkpoint.
     * Called on app launch.
     *
     * @return true if restored, false if no valid checkpoint
     */
    suspend fun tryRestore(): Boolean {
        return try {
            val checkpoint = checkpointDao.getCheckpoint()

            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = checkpoint != null,
                wasRecording = checkpoint?.wasRecording ?: false,
                lastCheckpointMs = checkpoint?.lastCheckpointMs ?: 0L,
                currentTimeMs = System.currentTimeMillis()
            )

            if (!decision.shouldRestore) {
                // Clear stale or invalid checkpoint
                if (checkpoint != null) {
                    checkpointDao.clearCheckpoint()
                    android.util.Log.i(TAG, "Cleared checkpoint: ${decision.reason}")
                }
                return false
            }

            // Restore data
            val state = json.decodeFromString<AccumulatorState>(checkpoint!!.accumulatorsJson)
            val snapshots = json.decodeFromString<List<RideSnapshot>>(checkpoint.snapshotsJson)

            liveDataCollector.restoreFromCheckpoint(state, snapshots)

            // Clear checkpoint after successful restore
            checkpointDao.clearCheckpoint()

            android.util.Log.i(TAG, "Restored checkpoint: samples=${state.sampleCount}, snapshots=${snapshots.size}")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to restore checkpoint: ${e.message}")
            // Clear corrupted checkpoint
            try {
                checkpointDao.clearCheckpoint()
            } catch (_: Exception) { }
            false
        }
    }

    /**
     * Clear checkpoint.
     * Called when ride ends normally.
     */
    suspend fun clearCheckpoint() {
        try {
            checkpointDao.clearCheckpoint()
            android.util.Log.d(TAG, "Cleared checkpoint")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to clear checkpoint: ${e.message}")
        }
    }

    /**
     * Emergency save for service destruction.
     * Uses blocking call since onDestroy is synchronous.
     */
    fun emergencySave(wasRecording: Boolean) {
        try {
            kotlinx.coroutines.runBlocking {
                saveCheckpoint(wasRecording)
            }
            android.util.Log.i(TAG, "Emergency checkpoint saved")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Emergency checkpoint failed: ${e.message}")
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        checkpointJob?.cancel()
        checkpointJob = null
        scope.cancel()
        android.util.Log.i(TAG, "Destroyed")
    }
}
