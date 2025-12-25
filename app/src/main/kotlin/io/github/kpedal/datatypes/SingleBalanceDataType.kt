package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics

/**
 * Layout 6: Single Balance
 * Full screen balance display with large numbers
 */
class SingleBalanceDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "single-balance") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_single_balance_small
        LayoutSize.MEDIUM -> R.layout.datatype_single_balance_medium
        LayoutSize.LARGE -> R.layout.datatype_single_balance_large
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

        // Progress bar - only in LARGE
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setProgressBar(R.id.balance_bar, 100, right, false)
        }
    }
}
