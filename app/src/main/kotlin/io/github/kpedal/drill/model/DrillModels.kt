package io.github.kpedal.drill.model

import androidx.compose.runtime.Immutable

/**
 * Types of drills available.
 */
enum class DrillType {
    TIMED_FOCUS,        // Fixed duration, focus on specific metric
    TARGET_BASED,       // Hold target for specified time
    GUIDED_WORKOUT      // Multi-phase structured workout
}

/**
 * Which metric the drill focuses on.
 */
enum class DrillMetric {
    BALANCE,
    TORQUE_EFFECTIVENESS,
    PEDAL_SMOOTHNESS,
    COMBINED              // Multiple metrics
}

/**
 * Difficulty level.
 */
enum class DrillDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

/**
 * Target condition for a drill phase.
 *
 * For BALANCE targets:
 * - Values are for RIGHT leg percentage (as stored in PedalingMetrics.balance)
 * - maxValue < 50 means LEFT leg should dominate (e.g., maxValue=48 → L>52%)
 * - minValue > 50 means RIGHT leg should dominate (e.g., minValue=52 → R>52%)
 * - targetValue = 50 means equal balance
 */
@Immutable
data class DrillTarget(
    val metric: DrillMetric,
    val minValue: Float? = null,      // e.g., PS >= 20
    val maxValue: Float? = null,      // e.g., TE <= 80
    val targetValue: Float? = null,   // e.g., Balance = 50
    val tolerance: Float = 0f         // e.g., Balance 50 ± 2
) {
    /**
     * Check if the current value meets the target.
     */
    fun isMet(value: Float): Boolean {
        // Target value with tolerance (e.g., Balance 50 ± 2)
        if (targetValue != null) {
            return value >= (targetValue - tolerance) && value <= (targetValue + tolerance)
        }

        // Range check
        val minOk = minValue == null || value >= minValue
        val maxOk = maxValue == null || value <= maxValue
        return minOk && maxOk
    }

    /**
     * Get descriptive text for this target.
     * For balance, translates to user-friendly L/R format.
     */
    fun description(): String {
        if (metric == DrillMetric.BALANCE) {
            return balanceDescription()
        }
        return when {
            targetValue != null && tolerance > 0 -> "${targetValue.toInt()}% ± ${tolerance.toInt()}"
            targetValue != null -> "${targetValue.toInt()}%"
            minValue != null && maxValue != null -> "${minValue.toInt()}-${maxValue.toInt()}%"
            minValue != null -> "≥${minValue.toInt()}%"
            maxValue != null -> "≤${maxValue.toInt()}%"
            else -> "any"
        }
    }

    /**
     * Get balance-specific description showing L/R values.
     */
    private fun balanceDescription(): String {
        return when {
            // Exact target with tolerance (e.g., 50 ± 2 → "48-52 / 48-52")
            targetValue != null && tolerance > 0 -> {
                val min = (targetValue - tolerance).toInt()
                val max = (targetValue + tolerance).toInt()
                "L:${100 - max}-${100 - min} R:$min-$max"
            }
            // Exact target
            targetValue != null -> {
                val left = (100 - targetValue).toInt()
                val right = targetValue.toInt()
                "L:$left R:$right"
            }
            // Left leg focus (right < 50)
            maxValue != null && maxValue < 50 -> {
                val left = (100 - maxValue).toInt()
                "L≥$left%"
            }
            // Right leg focus (right > 50)
            minValue != null && minValue > 50 -> {
                "R≥${minValue.toInt()}%"
            }
            // Range
            minValue != null && maxValue != null -> {
                "L:${(100 - maxValue).toInt()}-${(100 - minValue).toInt()} R:${minValue.toInt()}-${maxValue.toInt()}"
            }
            else -> "50/50"
        }
    }

    /**
     * Get the current metric value formatted for display.
     * For balance, returns both L and R values.
     */
    fun formatCurrentValue(value: Float): String {
        return if (metric == DrillMetric.BALANCE) {
            val left = (100 - value).toInt()
            val right = value.toInt()
            "L:$left R:$right"
        } else {
            "${value.toInt()}%"
        }
    }

    /**
     * Calculate proximity to target: -1.0 (far below) to 0.0 (in target) to 1.0 (far above).
     * Returns null if no clear target boundary.
     */
    fun calculateProximity(value: Float): Float? {
        // For target with tolerance
        if (targetValue != null) {
            val lowerBound = targetValue - tolerance
            val upperBound = targetValue + tolerance
            return when {
                value < lowerBound -> ((value - lowerBound) / 10f).coerceIn(-1f, 0f)
                value > upperBound -> ((value - upperBound) / 10f).coerceIn(0f, 1f)
                else -> 0f // In target
            }
        }

        // For range (min to max)
        if (minValue != null && maxValue != null) {
            return when {
                value < minValue -> ((value - minValue) / 10f).coerceIn(-1f, 0f)
                value > maxValue -> ((value - maxValue) / 10f).coerceIn(0f, 1f)
                else -> 0f
            }
        }

        // For min only
        if (minValue != null) {
            return if (value < minValue) {
                ((value - minValue) / 10f).coerceIn(-1f, 0f)
            } else {
                0f
            }
        }

        // For max only
        if (maxValue != null) {
            return if (value > maxValue) {
                ((value - maxValue) / 10f).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

        return null
    }
}

/**
 * A single phase within a drill.
 */
@Immutable
data class DrillPhase(
    val name: String,
    val description: String,
    val durationMs: Long,             // Phase duration
    val target: DrillTarget? = null,  // Optional target to hit
    val holdTimeMs: Long = 0,         // For target-based: time to hold target
    val instruction: String = ""      // Coaching instruction
)

/**
 * Definition of a drill.
 */
@Immutable
data class Drill(
    val id: String,
    val name: String,
    val description: String,
    val type: DrillType,
    val metric: DrillMetric,
    val difficulty: DrillDifficulty,
    val phases: List<DrillPhase>,
    val tips: List<String> = emptyList()
) {
    /**
     * Total duration of the drill.
     */
    val totalDurationMs: Long
        get() = phases.sumOf { it.durationMs }

    /**
     * Formatted total duration.
     */
    val durationFormatted: String
        get() {
            val seconds = totalDurationMs / 1000
            return when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
                else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            }
        }
}

/**
 * Current state during drill execution.
 */
enum class DrillExecutionStatus {
    NOT_STARTED,
    COUNTDOWN,      // 3-2-1 countdown before start
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * Real-time state during drill execution.
 */
@Immutable
data class DrillExecutionState(
    val drill: Drill,
    val status: DrillExecutionStatus = DrillExecutionStatus.NOT_STARTED,
    val currentPhaseIndex: Int = 0,
    val elapsedMs: Long = 0,                    // Total elapsed time
    val phaseElapsedMs: Long = 0,               // Elapsed in current phase
    val targetHoldMs: Long = 0,                 // Time spent in target zone
    val isInTarget: Boolean = false,            // Currently meeting target
    val score: Float = 0f,                      // Overall score (0-100)
    val countdownSeconds: Int = 3,              // For countdown state
    val hasTarget: Boolean = false,             // Current phase has a target
    val targetProximity: Float = 0f,            // -1 (far below) to 0 (in) to 1 (far above)
    val phaseEndingCountdown: Int = -1          // 3, 2, 1, -1 (not showing)
) {
    val currentPhase: DrillPhase?
        get() = drill.phases.getOrNull(currentPhaseIndex)

    val nextPhase: DrillPhase?
        get() = drill.phases.getOrNull(currentPhaseIndex + 1)

    val phaseRemainingMs: Long
        get() {
            val phase = currentPhase ?: return 0
            return (phase.durationMs - phaseElapsedMs).coerceAtLeast(0)
        }

    val phaseRemainingSeconds: Int
        get() = (phaseRemainingMs / 1000).toInt()

    val totalRemainingMs: Long
        get() = (drill.totalDurationMs - elapsedMs).coerceAtLeast(0)

    val progressPercent: Float
        get() = if (drill.totalDurationMs > 0) {
            (elapsedMs.toFloat() / drill.totalDurationMs * 100).coerceIn(0f, 100f)
        } else 0f

    val phaseProgressPercent: Float
        get() {
            val phase = currentPhase ?: return 0f
            return if (phase.durationMs > 0) {
                (phaseElapsedMs.toFloat() / phase.durationMs * 100).coerceIn(0f, 100f)
            } else 0f
        }

    /**
     * Format time remaining as MM:SS.
     */
    fun formatTimeRemaining(): String {
        val seconds = (totalRemainingMs / 1000).toInt()
        val mins = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(mins, secs)
    }

    /**
     * Format phase time remaining as SS.
     */
    fun formatPhaseTimeRemaining(): String {
        val seconds = (phaseRemainingMs / 1000).toInt()
        return "${seconds}s"
    }
}

/**
 * Result of a completed drill.
 */
@Immutable
data class DrillResult(
    val id: Long = 0,                   // Database ID (0 for new results)
    val drillId: String,
    val drillName: String,
    val timestamp: Long,
    val durationMs: Long,
    val score: Float,                   // 0-100 overall score
    val timeInTargetMs: Long,           // Total time in target zone
    val timeInTargetPercent: Float,     // Percentage of time in target
    val completed: Boolean,             // False if cancelled
    val phaseScores: List<Float> = emptyList()  // Score per phase
) {
    /**
     * Formatted duration.
     */
    val durationFormatted: String
        get() {
            val seconds = durationMs / 1000
            val mins = seconds / 60
            val secs = seconds % 60
            return "%d:%02d".format(mins, secs)
        }

    /**
     * Rating based on score.
     */
    val rating: String
        get() = when {
            score >= 90 -> "Excellent"
            score >= 75 -> "Good"
            score >= 60 -> "Fair"
            score >= 40 -> "Needs Work"
            else -> "Keep Practicing"
        }
}
