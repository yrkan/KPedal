package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Layout 4: Full Overview
 * Balance + TE + PS all in one compact view
 */
class FullOverviewDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "full-overview") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_full_overview_small
        LayoutSize.SMALL_WIDE -> R.layout.datatype_full_overview_small_wide
        LayoutSize.MEDIUM_WIDE -> R.layout.datatype_full_overview_small_wide  // 2-row wide uses SMALL_WIDE layout
        LayoutSize.MEDIUM -> R.layout.datatype_full_overview_medium
        LayoutSize.LARGE -> R.layout.datatype_full_overview_large
        LayoutSize.NARROW -> R.layout.datatype_full_overview_medium  // Narrow uses MEDIUM layout
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                // BAL + TE + PS horizontal - compact, use smaller sizes
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.separator, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.TERTIARY)
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                // BAL + TE + PS horizontal (48|52 | TE 76 | PS 22)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.separator, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                // Balance + TE + PS (3 rows) - TERTIARY for all to fit
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.te_left, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.te_right, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.ps_left, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.ps_right, config, TextSizeCalculator.Role.TERTIARY)
            }
            LayoutSize.LARGE -> {
                // Balance + TE + PS - full size
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_avg, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.te_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_avg, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.ps_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_right, config, TextSizeCalculator.Role.SECONDARY)
            }
        }
    }

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        if (!metrics.hasData) {
            // No data - show dashes for all values
            views.setTextViewText(R.id.balance_left, NO_DATA)
            views.setTextViewText(R.id.balance_right, NO_DATA)
            views.setTextColor(R.id.balance_left, StatusCalculator.COLOR_WHITE)
            views.setTextColor(R.id.balance_right, StatusCalculator.COLOR_WHITE)

            if (currentLayoutSize == LayoutSize.LARGE) {
                views.setProgressBar(R.id.balance_bar, 100, 50, false)
            }

            // TE and PS averages - in SMALL, SMALL_WIDE and MEDIUM_WIDE
            if (currentLayoutSize == LayoutSize.SMALL || currentLayoutSize == LayoutSize.SMALL_WIDE || currentLayoutSize == LayoutSize.MEDIUM_WIDE) {
                views.setTextViewText(R.id.te_avg, NO_DATA)
                views.setTextViewText(R.id.ps_avg, NO_DATA)
                views.setTextColor(R.id.te_avg, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.ps_avg, StatusCalculator.COLOR_WHITE)
            }

            // TE and PS L/R - MEDIUM, LARGE and NARROW
            if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE || currentLayoutSize == LayoutSize.NARROW) {
                views.setTextViewText(R.id.te_left, NO_DATA)
                views.setTextViewText(R.id.te_right, NO_DATA)
                views.setTextViewText(R.id.ps_left, NO_DATA)
                views.setTextViewText(R.id.ps_right, NO_DATA)
                views.setTextColor(R.id.te_left, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.te_right, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.ps_left, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.ps_right, StatusCalculator.COLOR_WHITE)

                if (currentLayoutSize == LayoutSize.LARGE) {
                    views.setTextViewText(R.id.te_avg, NO_DATA)
                    views.setTextViewText(R.id.ps_avg, NO_DATA)
                    views.setTextColor(R.id.te_avg, StatusCalculator.COLOR_WHITE)
                    views.setTextColor(R.id.ps_avg, StatusCalculator.COLOR_WHITE)
                    views.setProgressBar(R.id.te_bar, 100, 0, false)
                    views.setProgressBar(R.id.ps_bar, 50, 0, false)
                }
            }
            return
        }

        // Balance - in all layouts
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setProgressBar(R.id.balance_bar, 100, right, false)
        }

        // TE and PS averages - in SMALL, SMALL_WIDE and MEDIUM_WIDE
        if (currentLayoutSize == LayoutSize.SMALL || currentLayoutSize == LayoutSize.SMALL_WIDE || currentLayoutSize == LayoutSize.MEDIUM_WIDE) {
            val teAvg = metrics.torqueEffAvg.toInt()
            val psAvg = metrics.pedalSmoothAvg.toInt()
            val teAvgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)
            val psAvgStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)

            views.setTextViewText(R.id.te_avg, "$teAvg")
            views.setTextViewText(R.id.ps_avg, "$psAvg")
            views.setTextColor(R.id.te_avg, StatusCalculator.statusColor(teAvgStatus))
            views.setTextColor(R.id.ps_avg, StatusCalculator.statusColor(psAvgStatus))
        }

        // TE and PS with L/R - MEDIUM, LARGE and NARROW
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE || currentLayoutSize == LayoutSize.NARROW) {
            val teL = metrics.torqueEffLeft.toInt()
            val teR = metrics.torqueEffRight.toInt()
            val teAvg = metrics.torqueEffAvg.toInt()
            val teLStatus = StatusCalculator.teStatus(metrics.torqueEffLeft)
            val teRStatus = StatusCalculator.teStatus(metrics.torqueEffRight)

            views.setTextViewText(R.id.te_left, "$teL")
            views.setTextViewText(R.id.te_right, "$teR")
            views.setTextColor(R.id.te_left, StatusCalculator.textColor(teLStatus))
            views.setTextColor(R.id.te_right, StatusCalculator.textColor(teRStatus))

            // PS L/R values
            val psL = metrics.pedalSmoothLeft.toInt()
            val psR = metrics.pedalSmoothRight.toInt()
            val psLStatus = StatusCalculator.psStatus(metrics.pedalSmoothLeft)
            val psRStatus = StatusCalculator.psStatus(metrics.pedalSmoothRight)

            views.setTextViewText(R.id.ps_left, "$psL")
            views.setTextViewText(R.id.ps_right, "$psR")
            views.setTextColor(R.id.ps_left, StatusCalculator.textColor(psLStatus))
            views.setTextColor(R.id.ps_right, StatusCalculator.textColor(psRStatus))

            // Extra elements only in LARGE
            if (currentLayoutSize == LayoutSize.LARGE) {
                val teAvgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)
                val psAvg = metrics.pedalSmoothAvg.toInt()
                val psAvgStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)

                views.setTextViewText(R.id.te_avg, "$teAvg%")
                views.setTextColor(R.id.te_avg, StatusCalculator.statusColor(teAvgStatus))
                views.setTextViewText(R.id.ps_avg, "$psAvg%")
                views.setTextColor(R.id.ps_avg, StatusCalculator.statusColor(psAvgStatus))

                // Progress bars
                views.setProgressBar(R.id.te_bar, 100, teAvg, false)
                views.setProgressBar(R.id.ps_bar, 50, psAvg, false)
            }
        }
    }
}
