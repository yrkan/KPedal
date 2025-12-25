package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics

/**
 * Power + Balance Widget
 * Shows large power value with balance bar below
 */
class PowerBalanceDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "power-balance") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_power_balance_small
        LayoutSize.MEDIUM -> R.layout.datatype_power_balance_medium
        LayoutSize.LARGE -> R.layout.datatype_power_balance_large
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Power value - in all layouts
        views.setTextViewText(R.id.power_value, "${metrics.power}")

        // Balance - not in SMALL
        if (currentLayoutSize != LayoutSize.SMALL) {
            val left = metrics.balanceLeft.toInt()
            val right = metrics.balance.toInt()

            views.setTextViewText(R.id.balance_left, "$left")
            views.setTextViewText(R.id.balance_right, "$right")
            applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

            if (currentLayoutSize == LayoutSize.LARGE) {
                views.setProgressBar(R.id.balance_bar, 100, right, false)
            }
        }
    }
}
