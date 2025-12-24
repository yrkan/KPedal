package io.github.kpedal.engine

import androidx.compose.runtime.Immutable

/**
 * Data class representing current pedaling metrics.
 *
 * @property balance Right leg power percentage (0-100). Left = 100 - balance.
 * @property torqueEffLeft Left leg torque effectiveness (0-100%)
 * @property torqueEffRight Right leg torque effectiveness (0-100%)
 * @property pedalSmoothLeft Left leg pedal smoothness (0-100%)
 * @property pedalSmoothRight Right leg pedal smoothness (0-100%)
 * @property power Current power output in watts
 * @property cadence Current cadence in RPM
 * @property balance3s 3-second smoothed balance (right leg %)
 * @property balance10s 10-second smoothed balance (right leg %)
 * @property timestamp Timestamp of the measurement
 */
@Immutable
data class PedalingMetrics(
    val balance: Float = 50f,
    val torqueEffLeft: Float = 0f,
    val torqueEffRight: Float = 0f,
    val pedalSmoothLeft: Float = 0f,
    val pedalSmoothRight: Float = 0f,
    val power: Int = 0,
    val cadence: Int = 0,
    val balance3s: Float = 50f,
    val balance10s: Float = 50f,
    val timestamp: Long = 0L
) {
    /**
     * Left leg power percentage (100 - balance)
     */
    val balanceLeft: Float
        get() = 100f - balance

    /**
     * 3-second smoothed left balance
     */
    val balance3sLeft: Float
        get() = 100f - balance3s

    /**
     * 10-second smoothed left balance
     */
    val balance10sLeft: Float
        get() = 100f - balance10s

    /**
     * Average torque effectiveness
     */
    val torqueEffAvg: Float
        get() = (torqueEffLeft + torqueEffRight) / 2f

    /**
     * Average pedal smoothness
     */
    val pedalSmoothAvg: Float
        get() = (pedalSmoothLeft + pedalSmoothRight) / 2f

    /**
     * Check if we have valid data (at least balance)
     */
    val hasData: Boolean
        get() = timestamp > 0L
}
