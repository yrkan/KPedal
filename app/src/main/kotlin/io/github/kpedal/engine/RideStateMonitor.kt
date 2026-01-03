package io.github.kpedal.engine

import io.github.kpedal.KPedalExtension
import io.github.kpedal.data.RideRepository
import io.github.kpedal.data.SyncService
import io.github.kpedal.engine.checkpoint.RideCheckpointManager
import io.hammerhead.karooext.models.RideState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Status of the last save operation.
 */
sealed class SaveStatus {
    object Idle : SaveStatus()
    object Saving : SaveStatus()
    data class Success(val rideId: Long) : SaveStatus()
    data class Failed(val message: String) : SaveStatus()
}

/**
 * Monitors Karoo ride state to trigger auto-save on ride end.
 */
class RideStateMonitor(
    private val extension: KPedalExtension,
    private val rideRepository: RideRepository,
    private val liveDataCollector: LiveDataCollector,
    private val achievementChecker: AchievementChecker? = null,
    private val syncService: SyncService? = null,
    private val checkpointManager: RideCheckpointManager? = null
) {
    companion object {
        private const val TAG = "RideStateMonitor"
        private const val MIN_RIDE_DURATION_MS = 60_000L // 1 minute minimum to save
        private const val STATUS_DISPLAY_MS = 5000L // How long to show success/failure status
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rideStateConsumerId = AtomicReference<String?>(null)

    // Save status for UI feedback
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    // Track if we were recording (to detect Recording -> Idle transition)
    @Volatile
    private var wasRecording = false

    // Track ride start time for duration calculation
    @Volatile
    private var rideStartTimeMs: Long = 0L

    /**
     * Start monitoring ride state.
     */
    fun startMonitoring() {
        if (!extension.karooSystem.connected) {
            android.util.Log.w(TAG, "KarooSystem not connected")
            return
        }

        try {
            val consumerId = extension.karooSystem.addConsumer { rideState: RideState ->
                handleRideStateChange(rideState)
            }
            rideStateConsumerId.set(consumerId)
            android.util.Log.i(TAG, "Started monitoring ride state")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to add ride state consumer: ${e.message}")
        }
    }

    /**
     * Stop monitoring ride state.
     */
    fun stopMonitoring() {
        rideStateConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to remove consumer: ${e.message}")
            }
        }
    }

    private fun handleRideStateChange(rideState: RideState) {
        android.util.Log.d(TAG, "Ride state changed: $rideState")

        when (rideState) {
            is RideState.Recording -> {
                if (!wasRecording) {
                    // Ride started - start all monitoring components
                    wasRecording = true
                    rideStartTimeMs = System.currentTimeMillis()

                    // Notify SyncService that ride is in progress (prevents network sync during ride)
                    syncService?.notifyRideStateChanged(recording = true)

                    // Fetch latest settings from cloud before ride starts
                    scope.launch {
                        try {
                            val fetched = syncService?.fetchSettings() ?: false
                            android.util.Log.i(TAG, "Fetched cloud settings before ride: $fetched")
                        } catch (e: Exception) {
                            android.util.Log.w(TAG, "Failed to fetch settings: ${e.message}")
                        }
                    }

                    // Start streaming and monitoring only when ride begins
                    extension.pedalingEngine.startStreaming()
                    extension.alertManager.startMonitoring()
                    extension.pedalMonitor.startMonitoring()
                    liveDataCollector.startCollecting()

                    // Start periodic checkpoints for crash recovery
                    checkpointManager?.startPeriodicCheckpoints()

                    android.util.Log.i(TAG, "Ride started - began collecting")
                }
            }

            is RideState.Paused -> {
                // Paused - save checkpoint immediately for crash recovery
                android.util.Log.d(TAG, "Ride paused (auto=${rideState.auto})")
                scope.launch {
                    checkpointManager?.saveCheckpoint(wasRecording = true)
                }
            }

            is RideState.Idle -> {
                if (wasRecording) {
                    // Ride ended - stop monitoring and trigger auto-save
                    wasRecording = false
                    liveDataCollector.stopCollecting()

                    // Stop streaming to save resources when not riding
                    extension.pedalingEngine.stopStreaming()
                    extension.alertManager.stopMonitoring()
                    extension.pedalMonitor.stopMonitoring()

                    // Stop periodic checkpoints - ride ended normally
                    checkpointManager?.stopPeriodicCheckpoints()
                    scope.launch {
                        checkpointManager?.clearCheckpoint()
                    }

                    // Notify SyncService that ride ended (allows pending sync on network restore)
                    syncService?.notifyRideStateChanged(recording = false)

                    val durationMs = System.currentTimeMillis() - rideStartTimeMs
                    autoSaveRide(durationMs)

                    android.util.Log.i(TAG, "Ride ended - auto-saving")
                }
            }
        }
    }

    private fun autoSaveRide(durationMs: Long) {
        val liveData = liveDataCollector.liveData.value
        val actualDurationMs = liveDataCollector.getDurationMs()
        // Get snapshots BEFORE reset (they are cleared on reset)
        val snapshots = liveDataCollector.getSnapshots()

        // Don't save if no pedal data was received (prevents saving default 50/50 values)
        if (!liveData.hasData) {
            android.util.Log.i(TAG, "No pedal data received, skipping auto-save")
            liveDataCollector.reset()
            return
        }

        // Only save if we have meaningful data (at least 1 minute)
        if (actualDurationMs < MIN_RIDE_DURATION_MS || durationMs < MIN_RIDE_DURATION_MS) {
            android.util.Log.i(TAG, "Ride too short to save (duration=${durationMs}ms, actual=${actualDurationMs}ms)")
            liveDataCollector.reset()
            return
        }

        // Reset collector now that we have all the data
        liveDataCollector.reset()

        scope.launch {
            _saveStatus.value = SaveStatus.Saving
            try {
                val ride = rideRepository.saveRide(
                    liveData = liveData,
                    durationMs = durationMs,
                    savedManually = false
                )
                android.util.Log.i(TAG, "Auto-saved ride with ID: ${ride.id}, snapshots: ${snapshots.size}")
                _saveStatus.value = SaveStatus.Success(ride.id)

                // Trigger cloud sync with snapshots if available
                syncService?.onRideSavedWithSnapshots(ride.id, snapshots)

                // Check for new achievements
                checkAchievements(ride)

                // Reset status after delay
                delay(STATUS_DISPLAY_MS)
                _saveStatus.value = SaveStatus.Idle
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to auto-save ride: ${e.message}")
                _saveStatus.value = SaveStatus.Failed(e.message ?: "Unknown error")
                // Keep failure status visible longer
                delay(STATUS_DISPLAY_MS * 2)
                _saveStatus.value = SaveStatus.Idle
            }
        }
    }

    /**
     * Manually save the current ride.
     * Called from UI when user taps "Save" button.
     * @return true if save was initiated, false if no data or too short
     */
    fun manualSave(): Boolean {
        val liveData = liveDataCollector.liveData.value
        val durationMs = liveDataCollector.getDurationMs()
        // Get snapshots BEFORE any potential reset
        val snapshots = liveDataCollector.getSnapshots()

        // Don't save if no pedal data was received (prevents saving default 50/50 values)
        if (!liveData.hasData) {
            android.util.Log.w(TAG, "No pedal data received, cannot save")
            return false
        }

        // Require minimum duration to avoid junk data
        if (durationMs < MIN_RIDE_DURATION_MS) {
            android.util.Log.w(TAG, "Ride too short to save (duration=${durationMs}ms)")
            return false
        }

        scope.launch {
            _saveStatus.value = SaveStatus.Saving
            try {
                val ride = rideRepository.saveRide(
                    liveData = liveData,
                    durationMs = durationMs,
                    savedManually = true
                )
                android.util.Log.i(TAG, "Manually saved ride with ID: ${ride.id}, snapshots: ${snapshots.size}")
                _saveStatus.value = SaveStatus.Success(ride.id)

                // Trigger cloud sync with snapshots if available
                syncService?.onRideSavedWithSnapshots(ride.id, snapshots)

                // Check for new achievements
                checkAchievements(ride)

                // Reset status after delay
                delay(STATUS_DISPLAY_MS)
                _saveStatus.value = SaveStatus.Idle
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to manually save ride: ${e.message}")
                _saveStatus.value = SaveStatus.Failed(e.message ?: "Unknown error")
                delay(STATUS_DISPLAY_MS * 2)
                _saveStatus.value = SaveStatus.Idle
            }
        }
        return true
    }

    /**
     * Check for newly unlocked achievements after a ride.
     */
    private suspend fun checkAchievements(ride: io.github.kpedal.data.database.RideEntity) {
        val checker = achievementChecker ?: return
        try {
            val newAchievements = checker.checkAfterRide(ride)
            if (newAchievements.isNotEmpty()) {
                android.util.Log.i(TAG, "Unlocked ${newAchievements.size} achievements: ${newAchievements.map { it.id }}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check achievements: ${e.message}")
        }
    }

    /**
     * Check if ride is currently recording.
     * Used for emergency checkpoint save on service destroy.
     */
    fun isRecording(): Boolean = wasRecording

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopMonitoring()
        scope.cancel()
    }
}
