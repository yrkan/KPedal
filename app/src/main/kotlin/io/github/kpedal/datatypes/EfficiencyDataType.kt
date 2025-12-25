package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator

/**
 * Layout 3: Efficiency
 * TE (Torque Effectiveness) + PS (Pedal Smoothness)
 */
class EfficiencyDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "efficiency") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_efficiency_small
        LayoutSize.MEDIUM -> R.layout.datatype_efficiency_medium
        LayoutSize.LARGE -> R.layout.datatype_efficiency_large
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        val teAvg = metrics.torqueEffAvg.toInt()
        val teAvgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)

        // TE avg - in all layouts
        views.setTextViewText(R.id.te_avg, "$teAvg%")
        views.setTextColor(R.id.te_avg, StatusCalculator.statusColor(teAvgStatus))

        // Full details - not in SMALL
        if (currentLayoutSize != LayoutSize.SMALL) {
            val teL = metrics.torqueEffLeft.toInt()
            val teR = metrics.torqueEffRight.toInt()
            val teLStatus = StatusCalculator.teStatus(metrics.torqueEffLeft)
            val teRStatus = StatusCalculator.teStatus(metrics.torqueEffRight)

            views.setTextViewText(R.id.te_left, "$teL")
            views.setTextViewText(R.id.te_right, "$teR")
            views.setTextColor(R.id.te_left, StatusCalculator.textColor(teLStatus))
            views.setTextColor(R.id.te_right, StatusCalculator.textColor(teRStatus))

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
}
