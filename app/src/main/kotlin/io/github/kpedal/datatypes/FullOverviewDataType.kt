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

    override fun getLayoutResId() = R.layout.datatype_full_overview

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Balance
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)
        views.setProgressBar(R.id.balance_bar, 100, right, false)

        // TE values
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

        // PS values
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
    }
}
