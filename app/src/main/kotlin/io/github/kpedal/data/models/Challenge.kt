package io.github.kpedal.data.models

import java.util.Calendar

/**
 * Weekly challenge definitions.
 * Challenges rotate based on week number.
 */
object WeeklyChallenges {

    data class Challenge(
        val id: String,
        val name: String,
        val description: String,
        val target: Int,
        val unit: String,
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
        Challenge("rides_3", "Active Week", "Complete 3 rides", 3, "rides", ChallengeType.RIDES),
        Challenge("balance_52", "Balanced Rider", "Avg balance within 48-52%", 3, "rides", ChallengeType.BALANCE),
        Challenge("optimal_60", "Zone Master", "60%+ time in optimal zone", 60, "%", ChallengeType.ZONE),
        Challenge("drills_2", "Technique Focus", "Complete 2 drills", 2, "drills", ChallengeType.DRILLS),
        Challenge("streak_4", "Consistency", "Ride 4 days in a row", 4, "days", ChallengeType.STREAK),
        Challenge("te_75", "Efficient Pedaling", "Avg TE above 70%", 3, "rides", ChallengeType.TE),
        Challenge("ps_22", "Smooth Circles", "Avg PS above 20%", 3, "rides", ChallengeType.PS)
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
