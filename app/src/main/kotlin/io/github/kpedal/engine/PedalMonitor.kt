package io.github.kpedal.engine

import io.github.kpedal.data.models.PedalInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors pedal connection status and signal quality.
 * Tracks update frequency to determine signal quality.
 */
class PedalMonitor(
    private val pedalingEngine: PedalingEngine
) {
    companion object {
        private const val TAG = "PedalMonitor"
        private const val UPDATE_INTERVAL_MS = 1000L // Check every second
        private const val STALE_THRESHOLD_MS = 5000L // Consider stale after 5 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var monitoringJob: kotlinx.coroutines.Job? = null

    private val _pedalInfo = MutableStateFlow(PedalInfo())
    val pedalInfo: StateFlow<PedalInfo> = _pedalInfo.asStateFlow()

    // Track update times for frequency calculation
    private var lastUpdateCount = 0
    private var updateCount = 0
    private var lastCheckTime = System.currentTimeMillis()
    private var lastDataTime = 0L

    /**
     * Start monitoring pedal connection.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            // Observe metrics updates to track data freshness
            launch {
                pedalingEngine.metrics.collect { metrics ->
                    // Only count as update if we have any pedal data
                    if (metrics.balance != 50f || metrics.torqueEffLeft > 0 || metrics.pedalSmoothLeft > 0) {
                        updateCount++
                        lastDataTime = System.currentTimeMillis()
                    }
                }
            }

            // Periodically calculate update frequency and emit status
            while (isActive) {
                delay(UPDATE_INTERVAL_MS)
                updatePedalInfo()
            }
        }
        android.util.Log.i(TAG, "Started monitoring pedal connection")
    }

    /**
     * Stop monitoring.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        android.util.Log.i(TAG, "Stopped monitoring pedal connection")
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopMonitoring()
        scope.cancel()
    }

    private fun updatePedalInfo() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastCheckTime) / 1000f

        // Calculate updates per second
        val newUpdates = updateCount - lastUpdateCount
        val updateFrequency = if (elapsedSeconds > 0) newUpdates / elapsedSeconds else 0f

        // Determine connection status
        val isConnected = lastDataTime > 0 && (now - lastDataTime) < STALE_THRESHOLD_MS

        // Determine signal quality based on update frequency
        val signalQuality = when {
            !isConnected -> PedalInfo.SignalQuality.UNKNOWN
            updateFrequency >= 1f -> PedalInfo.SignalQuality.GOOD
            updateFrequency >= 0.5f -> PedalInfo.SignalQuality.FAIR
            else -> PedalInfo.SignalQuality.POOR
        }

        _pedalInfo.value = PedalInfo(
            isConnected = isConnected,
            signalQuality = signalQuality,
            lastDataReceivedMs = lastDataTime,
            updateFrequency = updateFrequency,
            modelName = "Power Meter Pedals" // Generic name since SDK doesn't provide model
        )

        // Reset counters for next interval
        lastUpdateCount = updateCount
        lastCheckTime = now
    }
}
