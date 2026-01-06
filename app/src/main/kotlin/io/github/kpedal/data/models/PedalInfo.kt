package io.github.kpedal.data.models

import io.github.kpedal.engine.SensorStreamState

/**
 * Information about pedal connection and signal quality.
 */
data class PedalInfo(
    val isConnected: Boolean = false,
    val sensorState: SensorStreamState = SensorStreamState.Idle,
    val signalQuality: SignalQuality = SignalQuality.UNKNOWN,
    val lastDataReceivedMs: Long = 0L,
    val updateFrequency: Float = 0f, // Updates per second
    val modelName: String = "Unknown",
    // Sensor diagnostics
    val sensors: List<SensorInfo> = emptyList(),
    val hasPowerMeter: Boolean = false,
    val hasCyclingDynamics: Boolean = false,
    // Active sensor ID (from streaming data deviceSourceIds, e.g., "48319" from "48319-11-5")
    val activeDeviceId: String? = null
) {
    /**
     * Signal quality based on update frequency.
     */
    enum class SignalQuality {
        UNKNOWN,
        POOR,      // < 0.5 updates/sec
        FAIR,      // 0.5-1 updates/sec
        GOOD       // > 1 update/sec
    }

    /**
     * Format the last data received timestamp as a human-readable string.
     */
    fun getLastDataAgo(): String {
        if (lastDataReceivedMs == 0L) return "Never"

        val now = System.currentTimeMillis()
        val diffMs = now - lastDataReceivedMs

        return when {
            diffMs < 1000 -> "Just now"
            diffMs < 60_000 -> "${diffMs / 1000}s ago"
            diffMs < 3600_000 -> "${diffMs / 60_000}m ago"
            else -> "More than 1h ago"
        }
    }

    /**
     * Get human-readable status string.
     */
    fun getStatusText(): String {
        return when (sensorState) {
            is SensorStreamState.Streaming -> when (signalQuality) {
                SignalQuality.GOOD -> "Connected - Good signal"
                SignalQuality.FAIR -> "Connected - Fair signal"
                SignalQuality.POOR -> "Connected - Poor signal"
                SignalQuality.UNKNOWN -> "Connected"
            }
            is SensorStreamState.Searching -> "Searching..."
            is SensorStreamState.NotAvailable -> "Not Available"
            is SensorStreamState.Disconnected -> "Disconnected"
            is SensorStreamState.Idle -> "Idle"
        }
    }
}

/**
 * Information about a single sensor from Karoo.
 */
data class SensorInfo(
    val id: String,
    val name: String,
    val connectionType: String,
    val enabled: Boolean,
    val supportedDataTypes: List<String>,
    val hasPowerBalance: Boolean,
    val hasTorqueEffectiveness: Boolean,
    val hasPedalSmoothness: Boolean
) {
    val hasCyclingDynamics: Boolean
        get() = hasTorqueEffectiveness || hasPedalSmoothness
}
