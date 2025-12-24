package io.github.kpedal.data.models

import io.github.kpedal.data.database.RideEntity

/**
 * Dashboard statistics for HomeScreen.
 */
data class DashboardData(
    val totalRides: Int = 0,
    val avgBalance: Float = 50f,
    val currentStreak: Int = 0,
    val lastRide: RideEntity? = null
) {
    val hasData: Boolean get() = totalRides > 0
}
