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

    override fun getLayoutResId() = R.layout.datatype_power_balance

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Power value
        views.setTextViewText(R.id.power_value, "${metrics.power}")

        // Balance values
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)
        views.setProgressBar(R.id.balance_bar, 100, right, false)
    }
}
