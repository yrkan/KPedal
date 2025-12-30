package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Balance Trend Widget
 * Shows current balance + 3s and 10s smoothed values
 */
class BalanceTrendDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "balance-trend") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_balance_trend_small
        LayoutSize.SMALL_WIDE -> R.layout.datatype_balance_trend_small_wide
        LayoutSize.MEDIUM_WIDE -> R.layout.datatype_balance_trend_small_wide  // 2-row wide uses SMALL_WIDE layout
        LayoutSize.MEDIUM -> R.layout.datatype_balance_trend_medium
        LayoutSize.LARGE -> R.layout.datatype_balance_trend_large
        LayoutSize.NARROW -> R.layout.datatype_balance_trend_medium  // Narrow uses MEDIUM layout
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        // Current balance: PRIMARY in all layouts
        views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.PRIMARY)

        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                // Label + trend indicator
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.trend_indicator, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.MEDIUM -> {
                // Current + 3s smoothed
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_balance_3s, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_3s_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_3s_right, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.LARGE -> {
                // Current + 3s + 10s smoothed
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_balance_3s, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_balance_10s, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_3s_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_3s_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_10s_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_10s_right, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.NARROW -> {
                // Same as MEDIUM (uses MEDIUM layout)
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_balance_3s, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_3s_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_3s_right, config, TextSizeCalculator.Role.SECONDARY)
            }
        }
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Current balance - in all layouts
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

        // Trend indicator - only in SMALL, SMALL_WIDE and MEDIUM_WIDE
        if (currentLayoutSize == LayoutSize.SMALL || currentLayoutSize == LayoutSize.SMALL_WIDE || currentLayoutSize == LayoutSize.MEDIUM_WIDE) {
            // Compare current balance with 3s smoothed to determine trend
            val currentRight = metrics.balance
            val smoothedRight = metrics.balance3s
            val diff = currentRight - smoothedRight

            val (arrow, color) = when {
                diff > 1.5f -> "↗" to StatusCalculator.COLOR_ATTENTION  // Moving right
                diff < -1.5f -> "↘" to StatusCalculator.COLOR_ATTENTION // Moving left
                else -> "→" to StatusCalculator.COLOR_OPTIMAL           // Stable
            }
            views.setTextViewText(R.id.trend_indicator, arrow)
            views.setTextColor(R.id.trend_indicator, color)
        }

        // 3s smoothed - MEDIUM, LARGE and NARROW
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE || currentLayoutSize == LayoutSize.NARROW) {
            val left3s = metrics.balance3sLeft.toInt()
            val right3s = metrics.balance3s.toInt()
            views.setTextViewText(R.id.balance_3s_left, "$left3s")
            views.setTextViewText(R.id.balance_3s_right, "$right3s")

            // 10s smoothed - only in LARGE
            if (currentLayoutSize == LayoutSize.LARGE) {
                val left10s = metrics.balance10sLeft.toInt()
                val right10s = metrics.balance10s.toInt()
                views.setTextViewText(R.id.balance_10s_left, "$left10s")
                views.setTextViewText(R.id.balance_10s_right, "$right10s")
            }
        }
    }
}
