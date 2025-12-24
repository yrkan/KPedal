package io.github.kpedal.datatypes

import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator

/**
 * Layout 1: Quick Glance
 * Status indicator (checkmark or warning) + Balance bar
 */
class QuickGlanceDataType(
    kpedalExtension: KPedalExtension
) : BaseDataType(kpedalExtension, "quick-glance") {

    override fun getLayoutResId() = R.layout.datatype_quick_glance

    override fun updateViews(views: RemoteViews, metrics: PedalingMetrics) {
        // Status indicator - show which metrics have issues
        val balanceOk = StatusCalculator.balanceStatus(metrics.balance) == StatusCalculator.Status.OPTIMAL
        val teOk = StatusCalculator.teStatus(metrics.torqueEffAvg) == StatusCalculator.Status.OPTIMAL
        val psOk = StatusCalculator.psStatus(metrics.pedalSmoothAvg) == StatusCalculator.Status.OPTIMAL

        val issues = mutableListOf<String>()
        if (!balanceOk) issues.add("Bal")
        if (!teOk) issues.add("TE")
        if (!psOk) issues.add("PS")

        if (issues.isEmpty()) {
            views.setTextViewText(R.id.status_icon, "✓")
            views.setTextColor(R.id.status_icon, StatusCalculator.COLOR_OPTIMAL)
            views.setTextViewText(R.id.status_text, "ALL GOOD")
        } else {
            views.setTextViewText(R.id.status_icon, "!")
            val color = if (issues.size >= 2) StatusCalculator.COLOR_PROBLEM else StatusCalculator.COLOR_ATTENTION
            views.setTextColor(R.id.status_icon, color)
            views.setTextViewText(R.id.status_text, issues.joinToString(", "))
        }

        // Balance
        val left = metrics.balanceLeft.toInt()
        val right = metrics.balance.toInt()

        views.setTextViewText(R.id.balance_left, "$left")
        views.setTextViewText(R.id.balance_right, "$right")
        applyBalanceColors(views, R.id.balance_left, R.id.balance_right, left, right)
        views.setProgressBar(R.id.balance_bar, 100, right, false)
    }
}
