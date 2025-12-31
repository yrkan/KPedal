package io.github.kpedal.data.models

import androidx.annotation.StringRes
import io.github.kpedal.R
import java.util.Calendar

/**
 * Weekly challenge definitions.
 * Challenges rotate based on week number.
 */
object WeeklyChallenges {

    data class Challenge(
        val id: String,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        val target: Int,
        @StringRes val unitRes: Int,
        val type: ChallengeType = ChallengeType.RIDES
    )

    enum class ChallengeType {
        RIDES,      // Count of rides
        BALANCE,    // Balance percentage target
        ZONE,       // Time in optimal zone
        DRILLS,     // Count of drills
        STREAK,     // Consecutive days
        TE,         // Torque effectiveness
        PS          // Pedal smoothness
    }

    val challenges = listOf(
        Challenge("rides_3", R.string.challenge_active_week, R.string.challenge_active_week_desc, 3, R.string.unit_rides, ChallengeType.RIDES),
        Challenge("balance_52", R.string.challenge_balanced_rider, R.string.challenge_balanced_rider_desc, 3, R.string.unit_rides, ChallengeType.BALANCE),
        Challenge("optimal_60", R.string.challenge_zone_master, R.string.challenge_zone_master_desc, 60, R.string.unit_percent, ChallengeType.ZONE),
        Challenge("drills_2", R.string.challenge_technique_focus, R.string.challenge_technique_focus_desc, 2, R.string.unit_drills, ChallengeType.DRILLS),
        Challenge("streak_4", R.string.challenge_consistency, R.string.challenge_consistency_desc, 4, R.string.unit_days, ChallengeType.STREAK),
        Challenge("te_75", R.string.challenge_efficient_pedaling, R.string.challenge_efficient_pedaling_desc, 3, R.string.unit_rides, ChallengeType.TE),
        Challenge("ps_22", R.string.challenge_smooth_circles, R.string.challenge_smooth_circles_desc, 3, R.string.unit_rides, ChallengeType.PS)
    )

    /**
     * Get current week's challenge.
     */
    fun getCurrentChallenge(): Challenge {
        val weekOfYear = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        val index = weekOfYear % challenges.size
        return challenges[index]
    }

    /**
     * Get next week's challenge preview.
     */
    fun getNextChallenge(): Challenge {
        val weekOfYear = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        val index = (weekOfYear + 1) % challenges.size
        return challenges[index]
    }

    /**
     * Get week start timestamp (Monday 00:00).
     */
    fun getWeekStartTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

/**
 * Progress tracking for current challenge.
 */
data class ChallengeProgress(
    val challenge: WeeklyChallenges.Challenge,
    val currentProgress: Int,
    val target: Int,
    val weekStart: Long
) {
    val progressPercent: Int
        get() = ((currentProgress.toFloat() / target) * 100).toInt().coerceIn(0, 100)

    val isComplete: Boolean
        get() = currentProgress >= target
}
