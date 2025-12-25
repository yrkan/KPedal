package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics

/**
 * Balance Trend Widget
 * Shows current balance + 3s and 10s smoothed values
 */
class BalanceTrendDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "balance-trend") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_balance_trend_small
        LayoutSize.MEDIUM -> R.layout.datatype_balance_trend_medium
        LayoutSize.LARGE -> R.layout.datatype_balance_trend_large
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Current balance - in all layouts
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

        // 3s smoothed - not in SMALL
        if (currentLayoutSize != LayoutSize.SMALL) {
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
