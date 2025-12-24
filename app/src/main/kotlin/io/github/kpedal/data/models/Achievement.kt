package io.github.kpedal.data.models

/**
 * Achievement definitions.
 */
sealed class Achievement(
    val id: String,
    val name: String,
    val description: String
) {
    // Ride count achievements
    object FirstRide : Achievement("first_ride", "First Ride", "Complete your first ride")
    object TenRides : Achievement("ten_rides", "Getting Started", "Complete 10 rides")
    object FiftyRides : Achievement("fifty_rides", "Dedicated", "Complete 50 rides")
    object HundredRides : Achievement("hundred_rides", "Century", "Complete 100 rides")

    // Balance achievements
    object PerfectBalance1m : Achievement("perfect_balance_1m", "Balanced", "1 min at optimal balance")
    object PerfectBalance5m : Achievement("perfect_balance_5m", "Well Balanced", "5 min at optimal balance")
    object PerfectBalance10m : Achievement("perfect_balance_10m", "Master of Balance", "10 min at optimal balance")

    // Efficiency achievements
    object EfficiencyMaster : Achievement("efficiency_master", "Efficient Rider", "TE + PS optimal for 5 min")
    object SmoothOperator : Achievement("smooth_operator", "Smooth Operator", "PS >= 25% for 10 min")

    // Streak achievements
    object ThreeDayStreak : Achievement("streak_3", "Getting Consistent", "3 day riding streak")
    object SevenDayStreak : Achievement("streak_7", "Weekly Warrior", "7 day riding streak")
    object FourteenDayStreak : Achievement("streak_14", "Two Week Hero", "14 day riding streak")
    object ThirtyDayStreak : Achievement("streak_30", "Monthly Master", "30 day riding streak")

    // Drill achievements
    object FirstDrill : Achievement("first_drill", "Practice Makes Perfect", "Complete your first drill")
    object TenDrills : Achievement("ten_drills", "Drill Enthusiast", "Complete 10 drills")
    object PerfectDrill : Achievement("perfect_drill", "Perfect Form", "Score 90%+ on any drill")

    companion object {
        val all: List<Achievement> = listOf(
            FirstRide, TenRides, FiftyRides, HundredRides,
            PerfectBalance1m, PerfectBalance5m, PerfectBalance10m,
            EfficiencyMaster, SmoothOperator,
            ThreeDayStreak, SevenDayStreak, FourteenDayStreak, ThirtyDayStreak,
            FirstDrill, TenDrills, PerfectDrill
        )

        fun getById(id: String): Achievement? = all.find { it.id == id }
    }
}

/**
 * Unlocked achievement data.
 */
data class UnlockedAchievement(
    val achievement: Achievement,
    val unlockedAt: Long,
    val progress: Int = 100
)
