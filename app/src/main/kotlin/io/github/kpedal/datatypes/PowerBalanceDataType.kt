package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Power + Balance Widget
 * Shows large power value with balance bar below
 */
class PowerBalanceDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "power-balance") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_power_balance_small
        LayoutSize.SMALL_WIDE -> R.layout.datatype_power_balance_small_wide
        LayoutSize.MEDIUM_WIDE -> R.layout.datatype_power_balance_small_wide  // 2-row wide uses SMALL_WIDE layout
        LayoutSize.MEDIUM -> R.layout.datatype_power_balance_medium
        LayoutSize.LARGE -> R.layout.datatype_power_balance_large
        LayoutSize.NARROW -> R.layout.datatype_power_balance_medium  // Narrow uses MEDIUM layout
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        // Power value: PRIMARY in all layouts
        views.setAdaptiveTextSize(R.id.power_value, config, TextSizeCalculator.Role.PRIMARY)

        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                // Power + Balance compact vertical
                views.setAdaptiveTextSize(R.id.label_power, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.power_unit, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.separator, config, TextSizeCalculator.Role.TERTIARY)
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                // Power + Balance horizontal (245W | 48|52)
                views.setAdaptiveTextSize(R.id.power_unit, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.separator, config, TextSizeCalculator.Role.TERTIARY)
            }
            LayoutSize.MEDIUM -> {
                // Power label + balance values
                views.setAdaptiveTextSize(R.id.label_power, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.LARGE -> {
                // Full layout with all labels
                views.setAdaptiveTextSize(R.id.label_power, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.label_left, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_right, config, TextSizeCalculator.Role.LABEL)
            }
            LayoutSize.NARROW -> {
                // Same as MEDIUM (uses MEDIUM layout)
                views.setAdaptiveTextSize(R.id.label_power, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
            }
        }
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Power value - in all layouts (always show, even without pedal data)
        views.setTextViewText(R.id.power_value, "${metrics.power}")

        // Balance - show dash when no pedal data available
        if (metrics.hasData) {
            val left = metrics.balanceLeft.toInt()
            val right = metrics.balance.toInt()
            views.setTextViewText(R.id.balance_left, "$left")
            views.setTextViewText(R.id.balance_right, "$right")
            applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

            // Progress bar - only in LARGE
            if (currentLayoutSize == LayoutSize.LARGE) {
                views.setProgressBar(R.id.balance_bar, 100, right, false)
            }
        } else {
            views.setTextViewText(R.id.balance_left, NO_DATA)
            views.setTextViewText(R.id.balance_right, NO_DATA)
            views.setTextColor(R.id.balance_left, StatusCalculator.COLOR_WHITE)
            views.setTextColor(R.id.balance_right, StatusCalculator.COLOR_WHITE)

            // Reset progress bar to center when no data
            if (currentLayoutSize == LayoutSize.LARGE) {
                views.setProgressBar(R.id.balance_bar, 100, 50, false)
            }
        }
    }
}
