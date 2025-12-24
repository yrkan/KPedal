package io.github.kpedal.data

import io.github.kpedal.data.database.RideEntity
import java.util.Calendar

/**
 * Calculates streak of consecutive days with good rides.
 * A "good" ride is one where zoneOptimal >= 50%.
 */
object StreakCalculator {

    /**
     * Calculate current streak from rides.
     * Streak = consecutive days (including today) with at least one ride having zoneOptimal >= 50%.
     *
     * @param rides List of rides ordered by timestamp DESC (newest first)
     * @return Number of consecutive days with good rides
     */
    fun calculateStreak(rides: List<RideEntity>): Int {
        if (rides.isEmpty()) return 0

        // Group rides by day
        val ridesByDay = rides.groupBy { ride ->
            getDayKey(ride.timestamp)
        }

        // Check if each day has at least one good ride (zoneOptimal >= 50%)
        val daysWithGoodRides = ridesByDay.filter { (_, dayRides) ->
            dayRides.any { it.zoneOptimal >= 50 }
        }.keys.sorted().reversed() // Newest first

        if (daysWithGoodRides.isEmpty()) return 0

        // Count consecutive days starting from today or yesterday
        val today = getDayKey(System.currentTimeMillis())
        val yesterday = today - 1

        // Streak must include today or yesterday to be valid
        val startDay = when {
            daysWithGoodRides.first() == today -> today
            daysWithGoodRides.first() == yesterday -> yesterday
            else -> return 0 // Streak broken
        }

        var streak = 0
        var expectedDay = startDay

        for (day in daysWithGoodRides) {
            if (day == expectedDay) {
                streak++
                expectedDay--
            } else if (day < expectedDay) {
                // Gap found, streak ends
                break
            }
        }

        return streak
    }

    /**
     * Get day key (days since epoch) for grouping rides by day.
     */
    private fun getDayKey(timestampMs: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis / (24 * 60 * 60 * 1000)
    }
}
