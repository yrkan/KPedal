package io.github.kpedal.engine

import kotlin.math.abs

/**
 * Calculator for pedaling metrics status.
 *
 * Status logic:
 * - OPTIMAL: Everything is in ideal range
 * - ATTENTION: Slightly outside optimal, monitor
 * - PROBLEM: Significantly outside range, needs correction
 */
object StatusCalculator {

    enum class Status {
        OPTIMAL,
        ATTENTION,
        PROBLEM
    }

    // Color constants (ARGB format)
    const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    const val COLOR_OPTIMAL = 0xFF4CAF50.toInt()    // Green
    const val COLOR_ATTENTION = 0xFFFFC107.toInt()  // Yellow/Amber
    const val COLOR_PROBLEM = 0xFFF44336.toInt()    // Red
    const val COLOR_DIM = 0xFF666666.toInt()        // Gray for labels

    // Default thresholds (can be overridden by user settings)
    private const val DEFAULT_BALANCE_OPTIMAL = 2f
    private const val DEFAULT_BALANCE_ATTENTION = 5f
    private const val DEFAULT_TE_MIN = 70f
    private const val DEFAULT_TE_MAX = 80f
    private const val DEFAULT_PS_MIN = 20f

    /**
     * Calculate balance status.
     *
     * Research:
     * - Pro cyclists: 48-52% (±2%)
     * - Amateurs: 45-55% (±5%) is normal
     * - 2-5% asymmetry is natural and changes with fatigue
     *
     * @param balance Right side percentage (0-100)
     * @param threshold User-defined threshold (default 5 means ±5% for attention)
     * @return Status based on deviation from 50%
     */
    fun balanceStatus(balance: Float, threshold: Int = DEFAULT_BALANCE_ATTENTION.toInt()): Status {
        val deviation = abs(50f - balance)
        val optimalThreshold = (threshold / 2f).coerceAtLeast(1f)
        val attentionThreshold = threshold.toFloat()
        return when {
            deviation <= optimalThreshold -> Status.OPTIMAL
            deviation <= attentionThreshold -> Status.ATTENTION
            else -> Status.PROBLEM
        }
    }

    /**
     * Calculate torque effectiveness status.
     *
     * Research (Wattbike):
     * - TE above 80% is NOT better!
     * - At TE > 80%, power on main (downstroke) phase decreases
     * - Optimal: 70-80%
     * - Too high TE = "stealing" power from the strong phase
     *
     * @param te Torque effectiveness (0-100%)
     * @param optimalMin Minimum of optimal range (default 70)
     * @param optimalMax Maximum of optimal range (default 80)
     * @return Status based on optimal range
     */
    fun teStatus(te: Float, optimalMin: Int = DEFAULT_TE_MIN.toInt(), optimalMax: Int = DEFAULT_TE_MAX.toInt()): Status {
        val min = optimalMin.toFloat()
        val max = optimalMax.toFloat()
        val attentionMargin = 5f
        return when {
            te in min..max -> Status.OPTIMAL
            te in (min - attentionMargin)..(max + attentionMargin) -> Status.ATTENTION
            else -> Status.PROBLEM
        }
    }

    /**
     * Calculate pedal smoothness status.
     *
     * Research:
     * - Good cyclists: 20-30%
     * - Elite: 25-35%
     * - 100% would be perfectly circular motion (unrealistic)
     * - Lower values during sprints are normal
     *
     * @param ps Pedal smoothness (0-100%)
     * @param minimum Minimum threshold for optimal (default 20)
     * @return Status based on minimum threshold
     */
    fun psStatus(ps: Float, minimum: Int = DEFAULT_PS_MIN.toInt()): Status {
        val minThreshold = minimum.toFloat()
        val attentionThreshold = (minimum - 5).coerceAtLeast(5).toFloat()
        return when {
            ps >= minThreshold -> Status.OPTIMAL
            ps >= attentionThreshold -> Status.ATTENTION
            else -> Status.PROBLEM
        }
    }

    /**
     * Get color for status.
     */
    fun statusColor(status: Status): Int = when (status) {
        Status.OPTIMAL -> COLOR_OPTIMAL
        Status.ATTENTION -> COLOR_ATTENTION
        Status.PROBLEM -> COLOR_PROBLEM
    }

    /**
     * Get text color for a metric value.
     * Returns white if within normal range, status color if needs attention.
     *
     * @param status The calculated status
     * @return White for optimal, status color otherwise
     */
    fun textColor(status: Status): Int = when (status) {
        Status.OPTIMAL -> COLOR_WHITE
        else -> statusColor(status)
    }

    /**
     * Count total issues across all metrics.
     */
    fun countIssues(
        metrics: PedalingMetrics,
        balanceThreshold: Int = DEFAULT_BALANCE_ATTENTION.toInt(),
        teMin: Int = DEFAULT_TE_MIN.toInt(),
        teMax: Int = DEFAULT_TE_MAX.toInt(),
        psMinimum: Int = DEFAULT_PS_MIN.toInt()
    ): Int {
        var count = 0
        if (balanceStatus(metrics.balance, balanceThreshold) != Status.OPTIMAL) count++
        if (teStatus(metrics.torqueEffAvg, teMin, teMax) != Status.OPTIMAL) count++
        if (psStatus(metrics.pedalSmoothAvg, psMinimum) != Status.OPTIMAL) count++
        return count
    }

    /**
     * Check if all metrics are optimal.
     */
    fun allOptimal(
        metrics: PedalingMetrics,
        balanceThreshold: Int = DEFAULT_BALANCE_ATTENTION.toInt(),
        teMin: Int = DEFAULT_TE_MIN.toInt(),
        teMax: Int = DEFAULT_TE_MAX.toInt(),
        psMinimum: Int = DEFAULT_PS_MIN.toInt()
    ): Boolean {
        return balanceStatus(metrics.balance, balanceThreshold) == Status.OPTIMAL &&
               teStatus(metrics.torqueEffAvg, teMin, teMax) == Status.OPTIMAL &&
               psStatus(metrics.pedalSmoothAvg, psMinimum) == Status.OPTIMAL
    }
}
