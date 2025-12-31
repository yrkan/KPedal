package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Layout 6: Single Balance
 * Full screen balance display with large numbers.
 *
 * Uses adaptive text sizes based on ViewConfig.textSize from Karoo SDK.
 */
class SingleBalanceDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "single-balance") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_single_balance_small
        LayoutSize.SMALL_WIDE -> R.layout.datatype_single_balance_small_wide
        LayoutSize.MEDIUM_WIDE -> R.layout.datatype_single_balance_small_wide  // 2-row wide uses SMALL_WIDE layout
        LayoutSize.MEDIUM -> R.layout.datatype_single_balance_medium
        LayoutSize.LARGE -> R.layout.datatype_single_balance_large
        LayoutSize.NARROW -> R.layout.datatype_single_balance_medium  // Narrow uses MEDIUM layout
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        // Primary values: balance left/right
        views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.PRIMARY)

        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                // Label + separator
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.separator, config, TextSizeCalculator.Role.SECONDARY)
                // Set localized labels
                views.setTextViewText(R.id.label_balance, getString(R.string.bal))
            }
            LayoutSize.MEDIUM, LayoutSize.LARGE, LayoutSize.NARROW -> {
                // Full labels (NARROW uses MEDIUM layout)
                views.setAdaptiveTextSize(R.id.label_header, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_left, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_right, config, TextSizeCalculator.Role.LABEL)
                // Set localized labels
                views.setTextViewText(R.id.label_header, getString(R.string.balance))
                views.setTextViewText(R.id.label_left, getString(R.string.left))
                views.setTextViewText(R.id.label_right, getString(R.string.right))
            }
        }
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
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

            if (currentLayoutSize == LayoutSize.LARGE) {
                views.setProgressBar(R.id.balance_bar, 100, 50, false)
            }
        }
    }
}
