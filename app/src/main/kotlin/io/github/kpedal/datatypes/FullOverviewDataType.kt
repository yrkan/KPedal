package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator

/**
 * Layout 4: Full Overview
 * Balance + TE + PS all in one compact view
 */
class FullOverviewDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "full-overview") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_full_overview_small
        LayoutSize.MEDIUM -> R.layout.datatype_full_overview_medium
        LayoutSize.LARGE -> R.layout.datatype_full_overview_large
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Balance - in all layouts
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setProgressBar(R.id.balance_bar, 100, right, false)
        }

        // TE - not in SMALL
        if (currentLayoutSize != LayoutSize.SMALL) {
            val teL = metrics.torqueEffLeft.toInt()
            val teR = metrics.torqueEffRight.toInt()
            val teAvg = metrics.torqueEffAvg.toInt()
            val teLStatus = StatusCalculator.teStatus(metrics.torqueEffLeft)
            val teRStatus = StatusCalculator.teStatus(metrics.torqueEffRight)
            val teAvgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)

            views.setTextViewText(R.id.te_left, "$teL")
            views.setTextViewText(R.id.te_right, "$teR")
            views.setTextViewText(R.id.te_avg, "$teAvg%")
            views.setTextColor(R.id.te_left, StatusCalculator.textColor(teLStatus))
            views.setTextColor(R.id.te_right, StatusCalculator.textColor(teRStatus))
            views.setTextColor(R.id.te_avg, StatusCalculator.statusColor(teAvgStatus))

            // PS and progress bars - only in LARGE
            if (currentLayoutSize == LayoutSize.LARGE) {
                val psL = metrics.pedalSmoothLeft.toInt()
                val psR = metrics.pedalSmoothRight.toInt()
                val psAvg = metrics.pedalSmoothAvg.toInt()
                val psLStatus = StatusCalculator.psStatus(metrics.pedalSmoothLeft)
                val psRStatus = StatusCalculator.psStatus(metrics.pedalSmoothRight)
                val psAvgStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)

                views.setTextViewText(R.id.ps_left, "$psL")
                views.setTextViewText(R.id.ps_right, "$psR")
                views.setTextViewText(R.id.ps_avg, "$psAvg%")
                views.setTextColor(R.id.ps_left, StatusCalculator.textColor(psLStatus))
                views.setTextColor(R.id.ps_right, StatusCalculator.textColor(psRStatus))
                views.setTextColor(R.id.ps_avg, StatusCalculator.statusColor(psAvgStatus))

                // TE and PS progress bars
                views.setProgressBar(R.id.te_bar, 100, teAvg, false)
                views.setProgressBar(R.id.ps_bar, 50, psAvg, false)
            }
        }
    }
}
