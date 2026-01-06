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
        // Set localized labels (RemoteViews don't use app locale in Karoo)
        views.setTextViewText(R.id.label_te, getString(R.string.te))
        views.setTextViewText(R.id.label_ps, getString(R.string.ps))

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
                // L/R labels only in LARGE
                views.setTextViewText(R.id.te_left_label, getString(R.string.left))
                views.setTextViewText(R.id.te_right_label, getString(R.string.right))
                views.setTextViewText(R.id.ps_left_label, getString(R.string.left))
                views.setTextViewText(R.id.ps_right_label, getString(R.string.right))
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

    override fun updateViews(
        views: RemoteViews,
        metrics: PedalingMetrics,
        layoutSize: LayoutSize,
        sensorDisconnected: Boolean
    ) {
        // Show "---" when sensor disconnected, "-" when no data
        val displayText = when {
            sensorDisconnected -> SENSOR_DISCONNECTED
            !metrics.hasData -> NO_DATA
            else -> null // Use actual values
        }

        if (displayText != null) {
            // No data or disconnected - show placeholder
            views.setTextViewText(R.id.te_avg, displayText)
            views.setTextColor(R.id.te_avg, StatusCalculator.COLOR_WHITE)

            if (layoutSize == LayoutSize.SMALL || layoutSize == LayoutSize.SMALL_WIDE || layoutSize == LayoutSize.MEDIUM_WIDE) {
                views.setTextViewText(R.id.ps_avg, displayText)
                views.setTextColor(R.id.ps_avg, StatusCalculator.COLOR_WHITE)
            }

            if (layoutSize == LayoutSize.MEDIUM || layoutSize == LayoutSize.LARGE || layoutSize == LayoutSize.NARROW) {
                views.setTextViewText(R.id.te_left, displayText)
                views.setTextViewText(R.id.te_right, displayText)
                views.setTextViewText(R.id.ps_left, displayText)
                views.setTextViewText(R.id.ps_right, displayText)
                views.setTextViewText(R.id.ps_avg, displayText)
                views.setTextColor(R.id.te_left, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.te_right, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.ps_left, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.ps_right, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.ps_avg, StatusCalculator.COLOR_WHITE)
            }
            return
        }

        val teAvg = metrics.torqueEffAvg.toInt()
        val teAvgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)

        // TE avg - format depends on layout (SMALL shows just number, others show %)
        val teText = if (layoutSize == LayoutSize.SMALL) "$teAvg" else "$teAvg%"
        views.setTextViewText(R.id.te_avg, teText)
        views.setTextColor(R.id.te_avg, StatusCalculator.statusColor(teAvgStatus))

        // PS avg - in SMALL, SMALL_WIDE and MEDIUM_WIDE
        if (layoutSize == LayoutSize.SMALL || layoutSize == LayoutSize.SMALL_WIDE || layoutSize == LayoutSize.MEDIUM_WIDE) {
            val psAvg = metrics.pedalSmoothAvg.toInt()
            val psAvgStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)
            val psText = if (layoutSize == LayoutSize.SMALL) "$psAvg" else "$psAvg%"
            views.setTextViewText(R.id.ps_avg, psText)
            views.setTextColor(R.id.ps_avg, StatusCalculator.statusColor(psAvgStatus))
        }

        // Full details - MEDIUM, LARGE and NARROW
        if (layoutSize == LayoutSize.MEDIUM || layoutSize == LayoutSize.LARGE || layoutSize == LayoutSize.NARROW) {
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
