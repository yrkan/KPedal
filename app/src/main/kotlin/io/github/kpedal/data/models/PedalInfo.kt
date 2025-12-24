package io.github.kpedal.data.models

/**
 * Information about pedal connection and signal quality.
 */
data class PedalInfo(
    val isConnected: Boolean = false,
    val signalQuality: SignalQuality = SignalQuality.UNKNOWN,
    val lastDataReceivedMs: Long = 0L,
    val updateFrequency: Float = 0f, // Updates per second
    val modelName: String = "Unknown"
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
        return if (isConnected) {
            when (signalQuality) {
                SignalQuality.GOOD -> "Connected - Good signal"
                SignalQuality.FAIR -> "Connected - Fair signal"
                SignalQuality.POOR -> "Connected - Poor signal"
                SignalQuality.UNKNOWN -> "Connected"
            }
        } else {
            "Disconnected"
        }
    }
}
