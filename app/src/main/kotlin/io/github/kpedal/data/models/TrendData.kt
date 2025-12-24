package io.github.kpedal.data.models

import io.github.kpedal.ui.components.charts.TrendPoint

/**
 * Aggregated trend data for analytics.
 */
data class TrendData(
    val period: Period = Period.WEEK,
    val balanceTrend: List<TrendPoint> = emptyList(),
    val teTrend: List<TrendPoint> = emptyList(),
    val psTrend: List<TrendPoint> = emptyList(),
    val avgBalance: Float = 50f,
    val avgTE: Float = 0f,
    val avgPS: Float = 0f,
    val rideCount: Int = 0,
    val progressScore: Int = 0  // -100 to +100, 0 = no change
) {
    val hasData: Boolean get() = rideCount > 0

    enum class Period(val label: String, val days: Int) {
        WEEK("7 Days", 7),
        MONTH("30 Days", 30)
    }
}

/**
 * Comparison data between two rides.
 */
data class RideComparison(
    val ride1: RideComparisonItem,
    val ride2: RideComparisonItem
) {
    data class RideComparisonItem(
        val id: Long,
        val dateLabel: String,
        val balance: Int,
        val teAvg: Int,
        val psAvg: Int,
        val zoneOptimal: Int,
        val duration: String
    )
}
