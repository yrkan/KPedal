package io.github.kpedal.data.models

import androidx.annotation.StringRes
import io.github.kpedal.R

/**
 * Achievement definitions.
 * Uses @StringRes for localization.
 */
sealed class Achievement(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int
) {
    // Ride count achievements
    object FirstRide : Achievement("first_ride", R.string.achievement_first_ride, R.string.achievement_first_ride_desc)
    object TenRides : Achievement("ten_rides", R.string.achievement_getting_started, R.string.achievement_getting_started_desc)
    object FiftyRides : Achievement("fifty_rides", R.string.achievement_dedicated, R.string.achievement_dedicated_desc)
    object HundredRides : Achievement("hundred_rides", R.string.achievement_century, R.string.achievement_century_desc)

    // Balance achievements
    object PerfectBalance1m : Achievement("perfect_balance_1m", R.string.achievement_balanced, R.string.achievement_balanced_desc)
    object PerfectBalance5m : Achievement("perfect_balance_5m", R.string.achievement_well_balanced, R.string.achievement_well_balanced_desc)
    object PerfectBalance10m : Achievement("perfect_balance_10m", R.string.achievement_master_of_balance, R.string.achievement_master_of_balance_desc)

    // Efficiency achievements
    object EfficiencyMaster : Achievement("efficiency_master", R.string.achievement_efficient_rider, R.string.achievement_efficient_rider_desc)
    object SmoothOperator : Achievement("smooth_operator", R.string.achievement_smooth_operator, R.string.achievement_smooth_operator_desc)

    // Streak achievements
    object ThreeDayStreak : Achievement("streak_3", R.string.achievement_getting_consistent, R.string.achievement_getting_consistent_desc)
    object SevenDayStreak : Achievement("streak_7", R.string.achievement_weekly_warrior, R.string.achievement_weekly_warrior_desc)
    object FourteenDayStreak : Achievement("streak_14", R.string.achievement_two_week_hero, R.string.achievement_two_week_hero_desc)
    object ThirtyDayStreak : Achievement("streak_30", R.string.achievement_monthly_master, R.string.achievement_monthly_master_desc)

    // Drill achievements
    object FirstDrill : Achievement("first_drill", R.string.achievement_practice_perfect, R.string.achievement_practice_perfect_desc)
    object TenDrills : Achievement("ten_drills", R.string.achievement_drill_enthusiast, R.string.achievement_drill_enthusiast_desc)
    object PerfectDrill : Achievement("perfect_drill", R.string.achievement_perfect_form, R.string.achievement_perfect_form_desc)

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
@androidx.compose.runtime.Immutable
data class UnlockedAchievement(
    val achievement: Achievement,
    val unlockedAt: Long,
    val progress: Int = 100
)
