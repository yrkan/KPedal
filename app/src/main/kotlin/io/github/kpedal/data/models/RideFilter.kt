package io.github.kpedal.data.models

import androidx.compose.runtime.Immutable
import io.github.kpedal.R

/**
 * Filter criteria for ride history.
 */
@Immutable
data class RideFilter(
    val dateRange: DateRange = DateRange.ALL
) {
    val isActive: Boolean get() = dateRange != DateRange.ALL

    enum class DateRange(val labelResId: Int, val daysBack: Int?) {
        LAST_WEEK(R.string.filter_7d, 7),
        LAST_MONTH(R.string.filter_30d, 30),
        ALL(R.string.filter_all, null)
    }
}
