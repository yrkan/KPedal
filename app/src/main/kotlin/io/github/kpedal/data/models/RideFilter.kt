package io.github.kpedal.data.models

/**
 * Filter criteria for ride history.
 */
data class RideFilter(
    val dateRange: DateRange = DateRange.ALL,
    val balanceFilter: BalanceFilter = BalanceFilter.ALL
) {
    val isActive: Boolean get() = dateRange != DateRange.ALL || balanceFilter != BalanceFilter.ALL

    enum class DateRange(val label: String, val daysBack: Int?) {
        LAST_WEEK("Last Week", 7),
        LAST_MONTH("Last Month", 30),
        ALL("All Time", null)
    }

    enum class BalanceFilter(val label: String) {
        ALL("All"),
        OPTIMAL_ONLY("Optimal Only"),
        PROBLEM_ONLY("Problem Only")
    }
}
