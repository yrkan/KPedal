package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Layout 3: Efficiency
 * TE (Torque Effectiveness) + PS (Pedal Smoothness)
 */
class EfficiencyDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "efficiency") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_efficiency_small
        LayoutSize.SMALL_WIDE -> R.layout.datatype_efficiency_small_wide
        LayoutSize.MEDIUM_WIDE -> R.layout.datatype_efficiency_small_wide  // 2-row wide uses SMALL_WIDE layout
        LayoutSize.MEDIUM -> R.layout.datatype_efficiency_medium
        LayoutSize.LARGE -> R.layout.datatype_efficiency_large
        LayoutSize.NARROW -> R.layout.datatype_efficiency_medium  // Narrow uses MEDIUM layout
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                // TE + PS horizontal - both are primary values
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.PRIMARY)
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                // TE + PS horizontal (TE 76% | PS 22%) - both are primary values
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.PRIMARY)
            }
            LayoutSize.MEDIUM -> {
                // TE + PS sections with L/R values
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.te_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.te_right, config, TextSizeCalculator.Role.PRIMARY)

                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.ps_right, config, TextSizeCalculator.Role.PRIMARY)
            }
            LayoutSize.LARGE -> {
                // Full layout with L/R labels - te_avg and ps_avg are small header values
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.te_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.te_right, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.te_left_label, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_right_label, config, TextSizeCalculator.Role.LABEL)

                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.ps_right, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.ps_left_label, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_right_label, config, TextSizeCalculator.Role.LABEL)
            }
            LayoutSize.NARROW -> {
                // Same as MEDIUM (uses MEDIUM layout)
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.te_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.te_right, config, TextSizeCalculator.Role.PRIMARY)

                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.ps_right, config, TextSizeCalculator.Role.PRIMARY)
            }
        }
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        val teAvg = metrics.torqueEffAvg.toInt()
        val teAvgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)

        // TE avg - format depends on layout (SMALL shows just number, others show %)
        val teText = if (currentLayoutSize == LayoutSize.SMALL) "$teAvg" else "$teAvg%"
        views.setTextViewText(R.id.te_avg, teText)
        views.setTextColor(R.id.te_avg, StatusCalculator.statusColor(teAvgStatus))

        // PS avg - in SMALL, SMALL_WIDE and MEDIUM_WIDE
        if (currentLayoutSize == LayoutSize.SMALL || currentLayoutSize == LayoutSize.SMALL_WIDE || currentLayoutSize == LayoutSize.MEDIUM_WIDE) {
            val psAvg = metrics.pedalSmoothAvg.toInt()
            val psAvgStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)
            val psText = if (currentLayoutSize == LayoutSize.SMALL) "$psAvg" else "$psAvg%"
            views.setTextViewText(R.id.ps_avg, psText)
            views.setTextColor(R.id.ps_avg, StatusCalculator.statusColor(psAvgStatus))
        }

        // Full details - MEDIUM, LARGE and NARROW
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE || currentLayoutSize == LayoutSize.NARROW) {
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
