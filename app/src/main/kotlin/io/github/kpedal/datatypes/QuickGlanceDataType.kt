package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Layout 1: Quick Glance
 * Status indicator (checkmark or warning) + Balance bar.
 *
 * Uses adaptive text sizes based on ViewConfig.textSize from Karoo SDK.
 */
class QuickGlanceDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "quick-glance") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_quick_glance_small
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_quick_glance_medium  // Narrow uses MEDIUM
        LayoutSize.LARGE -> R.layout.datatype_quick_glance_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                // ImageView icon + status text (no text size for ImageView)
                views.setAdaptiveTextSize(R.id.status_text, config, TextSizeCalculator.Role.PRIMARY)
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                // Icon + status text + balance (NARROW uses MEDIUM layout)
                views.setAdaptiveTextSize(R.id.status_icon, config, TextSizeCalculator.Role.ICON)
                views.setAdaptiveTextSize(R.id.status_text, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.LARGE -> {
                // Full layout with labels
                views.setAdaptiveTextSize(R.id.status_icon, config, TextSizeCalculator.Role.ICON)
                views.setAdaptiveTextSize(R.id.status_text, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_header, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.label_left, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_right, config, TextSizeCalculator.Role.LABEL)
                // Set localized labels
                views.setTextViewText(R.id.label_header, getString(R.string.balance))
                views.setTextViewText(R.id.label_left, getString(R.string.left))
                views.setTextViewText(R.id.label_right, getString(R.string.right))
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
            // No data or disconnected - show placeholder for status
            if (layoutSize == LayoutSize.SMALL || layoutSize == LayoutSize.SMALL_WIDE || layoutSize == LayoutSize.MEDIUM_WIDE) {
                views.setImageViewResource(R.id.status_icon_image, R.drawable.ic_warning)
            } else {
                views.setTextViewText(R.id.status_icon, displayText)
                views.setTextColor(R.id.status_icon, StatusCalculator.COLOR_WHITE)
            }
            views.setTextViewText(R.id.status_text, displayText)
            views.setTextColor(R.id.status_text, StatusCalculator.COLOR_WHITE)

            if (layoutSize == LayoutSize.MEDIUM || layoutSize == LayoutSize.LARGE || layoutSize == LayoutSize.NARROW) {
                views.setTextViewText(R.id.balance_left, displayText)
                views.setTextViewText(R.id.balance_right, displayText)
                views.setTextColor(R.id.balance_left, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.balance_right, StatusCalculator.COLOR_WHITE)
                if (layoutSize == LayoutSize.LARGE) {
                    views.setProgressBar(R.id.balance_bar, 100, 50, false)
                }
            }
            return
        }

        // Status indicator - show which metrics have issues
        val balanceOk = StatusCalculator.balanceStatus(metrics.balance) == StatusCalculator.Status.OPTIMAL
        val teOk = StatusCalculator.teStatus(metrics.torqueEffAvg) == StatusCalculator.Status.OPTIMAL
        val psOk = StatusCalculator.psStatus(metrics.pedalSmoothAvg) == StatusCalculator.Status.OPTIMAL

        val issues = mutableListOf<String>()
        if (!balanceOk) issues.add("Bal")
        if (!teOk) issues.add("TE")
        if (!psOk) issues.add("PS")

        if (issues.isEmpty()) {
            // SMALL/SMALL_WIDE/MEDIUM_WIDE uses ImageView, MEDIUM/LARGE use TextView
            if (layoutSize == LayoutSize.SMALL || layoutSize == LayoutSize.SMALL_WIDE || layoutSize == LayoutSize.MEDIUM_WIDE) {
                views.setImageViewResource(R.id.status_icon_image, R.drawable.ic_check)
            } else {
                views.setTextViewText(R.id.status_icon, "✓")
                views.setTextColor(R.id.status_icon, StatusCalculator.COLOR_OPTIMAL)
            }
            val okText = getString(R.string.all_good)
            views.setTextViewText(R.id.status_text, okText)
            views.setTextColor(R.id.status_text, StatusCalculator.COLOR_OPTIMAL)
        } else {
            val color = if (issues.size >= 2) StatusCalculator.COLOR_PROBLEM else StatusCalculator.COLOR_ATTENTION
            // SMALL/SMALL_WIDE/MEDIUM_WIDE uses ImageView, MEDIUM/LARGE use TextView
            if (layoutSize == LayoutSize.SMALL || layoutSize == LayoutSize.SMALL_WIDE || layoutSize == LayoutSize.MEDIUM_WIDE) {
                views.setImageViewResource(R.id.status_icon_image, R.drawable.ic_warning)
            } else {
                views.setTextViewText(R.id.status_icon, "!")
                views.setTextColor(R.id.status_icon, color)
            }
            views.setTextViewText(R.id.status_text, issues.joinToString(", "))
            views.setTextColor(R.id.status_text, color)
        }

        // Balance - MEDIUM, LARGE and NARROW
        if (layoutSize == LayoutSize.MEDIUM || layoutSize == LayoutSize.LARGE || layoutSize == LayoutSize.NARROW) {
            val left = metrics.balanceLeft.toInt()
            val right = metrics.balance.toInt()

            views.setTextViewText(R.id.balance_left, "$left")
            views.setTextViewText(R.id.balance_right, "$right")
            applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)

            if (layoutSize == LayoutSize.LARGE) {
                views.setProgressBar(R.id.balance_bar, 100, right, false)
            }
        }
    }
}
