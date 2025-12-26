package io.github.kpedal.engine

/**
 * Per-minute snapshot of ride metrics.
 * Stored in cloud only (not local database) for time-series charts.
 */
data class RideSnapshot(
    val minuteIndex: Int,         // 0, 1, 2...
    val timestamp: Long,          // Unix ms
    val balanceLeft: Int,         // L/R balance (avg for this minute)
    val balanceRight: Int,
    val teLeft: Int,              // Torque Effectiveness
    val teRight: Int,
    val psLeft: Int,              // Pedal Smoothness
    val psRight: Int,
    val powerAvg: Int,            // Power in watts
    val cadenceAvg: Int,          // Cadence in RPM
    val heartRateAvg: Int,        // Heart rate in BPM
    val zoneStatus: String,       // "OPTIMAL", "ATTENTION", "PROBLEM" (dominant zone for this minute)
)
