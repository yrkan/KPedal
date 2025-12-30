package io.github.kpedal.datatypes

import android.content.Context
import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.engine.StatusCalculator
import io.github.kpedal.ui.screens.LiveRideData
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Live DataType - Shows aggregated ride stats with Time In Zone.
 * Similar to the Live screen in the app but as a Profile data field.
 */
class LiveDataType(
    private val kpedalExtension: KPedalExtension
) : DataTypeImpl("kpedal", "live") {

    enum class LayoutSize {
        SMALL, SMALL_WIDE, MEDIUM_WIDE, MEDIUM, LARGE, NARROW
    }

    companion object {
        private const val TAG = "LiveDataType"
        private const val VIEW_UPDATE_INTERVAL_MS = 1000L

        /**
         * Sample live data for preview mode.
         */
        val PREVIEW_LIVE_DATA = LiveRideData(
            duration = "0:05:30",
            balanceLeft = 49,
            balanceRight = 51,
            teLeft = 74,
            teRight = 76,
            psLeft = 22,
            psRight = 24,
            zoneOptimal = 65,
            zoneAttention = 25,
            zoneProblem = 10,
            hasData = true
        )

        fun getLayoutSize(config: ViewConfig): LayoutSize {
            val isFullWidth = config.gridSize.first >= 50  // 60 = full, 30 = half
            val height = config.viewSize.second

            return if (isFullWidth) {
                // Full width layouts
                when {
                    height >= 250 -> LayoutSize.LARGE       // 1-3 fields stacked
                    height >= 160 -> LayoutSize.MEDIUM_WIDE // 4 fields (2-row wide)
                    else -> LayoutSize.SMALL_WIDE           // 5-6 fields (1-row wide)
                }
            } else {
                // Half width layouts (side by side)
                when {
                    height >= 600 -> LayoutSize.NARROW      // 2 fields side by side (tall, narrow)
                    height >= 200 -> LayoutSize.MEDIUM      // 2x2, 2x3 grid
                    else -> LayoutSize.SMALL                // 2x4+ grid cells (minimal content)
                }
            }
        }
    }

    private var viewScope: CoroutineScope? = null
    private var currentLayoutSize: LayoutSize = LayoutSize.MEDIUM

    private fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_live_small
        LayoutSize.SMALL_WIDE -> R.layout.datatype_live_small_wide
        LayoutSize.MEDIUM_WIDE -> R.layout.datatype_live_small_wide  // 2-row wide uses SMALL_WIDE layout
        LayoutSize.MEDIUM -> R.layout.datatype_live_medium
        LayoutSize.LARGE -> R.layout.datatype_live_large
        LayoutSize.NARROW -> R.layout.datatype_live_medium  // Narrow uses MEDIUM layout
    }

    /**
     * Apply adaptive text sizes based on ViewConfig.
     */
    private fun applyAdaptiveTextSizes(views: RemoteViews, config: ViewConfig) {
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                // Time in Zone: label + zone bar + optimal %
                views.setAdaptiveTextSize(R.id.label_ride, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.zone_optimal, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.zone_unit, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                // Balance + TE/PS (NARROW uses MEDIUM layout)
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.te_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_right, config, TextSizeCalculator.Role.SECONDARY)
            }
            LayoutSize.LARGE -> {
                // Balance + TE/PS + Time in Zone
                views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.balance_right, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.label_balance, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_te, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.label_ps, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.te_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.te_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_left, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.ps_right, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.label_time_in_zone, config, TextSizeCalculator.Role.LABEL)
                views.setAdaptiveTextSize(R.id.zone_optimal, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.zone_attention, config, TextSizeCalculator.Role.SECONDARY)
                views.setAdaptiveTextSize(R.id.zone_problem, config, TextSizeCalculator.Role.SECONDARY)
            }
        }
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        emitter.onNext(StreamState.Streaming(
            DataPoint(dataTypeId = dataTypeId, values = emptyMap())
        ))
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        viewScope?.cancel()
        viewScope = null

        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        currentLayoutSize = getLayoutSize(config)
        android.util.Log.d(TAG, "Grid: ${config.gridSize}, Size: $currentLayoutSize")

        val cachedViews = RemoteViews(context.packageName, getLayoutResId(currentLayoutSize))

        // Apply adaptive text sizes based on ViewConfig
        applyAdaptiveTextSizes(cachedViews, config)

        // Preview mode: render with sample data
        if (config.preview) {
            try {
                updateViews(cachedViews, PREVIEW_LIVE_DATA)
                emitter.updateView(cachedViews)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Preview update error: ${e.message}", e)
            }
            emitter.setCancellable { }
            return
        }

        // Live mode
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        viewScope = scope

        // Initial update
        scope.launch {
            try {
                val liveData = try {
                    kpedalExtension.pedalingEngine.liveDataCollector.liveData.value
                } catch (e: Exception) {
                    PREVIEW_LIVE_DATA
                }
                updateViews(cachedViews, liveData)
                emitter.updateView(cachedViews)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Initial view update error: ${e.message}")
                try {
                    updateViews(cachedViews, PREVIEW_LIVE_DATA)
                    emitter.updateView(cachedViews)
                } catch (e2: Exception) {
                    android.util.Log.e(TAG, "Fallback update failed: ${e2.message}")
                }
            }
        }

        // Rate-limited updates at 1Hz
        scope.launch {
            try {
                while (isActive) {
                    delay(VIEW_UPDATE_INTERVAL_MS)

                    val liveData = try {
                        kpedalExtension.pedalingEngine.liveDataCollector.liveData.value
                    } catch (e: Exception) {
                        continue
                    }
                    updateViews(cachedViews, liveData)
                    emitter.updateView(cachedViews)
                }
            } catch (e: Exception) {
                if (isActive) {
                    android.util.Log.w(TAG, "View update loop error: ${e.message}")
                }
            }
        }

        emitter.setCancellable {
            viewScope?.cancel()
            viewScope = null
        }
    }

    private fun updateViews(views: RemoteViews, liveData: LiveRideData) {
        // SMALL/SMALL_WIDE/MEDIUM_WIDE: Time in Zone only
        if (currentLayoutSize == LayoutSize.SMALL || currentLayoutSize == LayoutSize.SMALL_WIDE || currentLayoutSize == LayoutSize.MEDIUM_WIDE) {
            // Zone optimal percentage
            views.setTextViewText(R.id.zone_optimal, "${liveData.zoneOptimal}")

            // Update zone bar weights based on percentages
            // The bars use layout_weight, so we need to update visibility for 0% zones
            views.setProgressBar(R.id.zone_bar_optimal, 100, if (liveData.zoneOptimal > 0) 100 else 0, false)
            views.setProgressBar(R.id.zone_bar_attention, 100, if (liveData.zoneAttention > 0) 100 else 0, false)
            views.setProgressBar(R.id.zone_bar_problem, 100, if (liveData.zoneProblem > 0) 100 else 0, false)
            return
        }

        // MEDIUM and LARGE: Balance + TE/PS
        views.setTextViewText(R.id.balance_left, "${liveData.balanceLeft}")
        views.setTextViewText(R.id.balance_right, "${liveData.balanceRight}")

        val balanceStatus = StatusCalculator.balanceStatus(liveData.balanceRight.toFloat())
        if (balanceStatus != StatusCalculator.Status.OPTIMAL) {
            val statusColor = StatusCalculator.statusColor(balanceStatus)
            if (liveData.balanceLeft > 52) {
                views.setTextColor(R.id.balance_left, statusColor)
                views.setTextColor(R.id.balance_right, StatusCalculator.COLOR_WHITE)
            } else {
                views.setTextColor(R.id.balance_left, StatusCalculator.COLOR_WHITE)
                views.setTextColor(R.id.balance_right, statusColor)
            }
        } else {
            views.setTextColor(R.id.balance_left, StatusCalculator.COLOR_WHITE)
            views.setTextColor(R.id.balance_right, StatusCalculator.COLOR_WHITE)
        }

        // Balance bar
        views.setProgressBar(R.id.balance_bar, 100, liveData.balanceRight, false)

        // TE and PS
        if (currentLayoutSize != LayoutSize.SMALL) {
            views.setTextViewText(R.id.te_left, "${liveData.teLeft}")
            views.setTextViewText(R.id.te_right, "${liveData.teRight}")
            val teLStatus = StatusCalculator.teStatus(liveData.teLeft.toFloat())
            val teRStatus = StatusCalculator.teStatus(liveData.teRight.toFloat())
            views.setTextColor(R.id.te_left, StatusCalculator.textColor(teLStatus))
            views.setTextColor(R.id.te_right, StatusCalculator.textColor(teRStatus))

            views.setTextViewText(R.id.ps_left, "${liveData.psLeft}")
            views.setTextViewText(R.id.ps_right, "${liveData.psRight}")
            val psLStatus = StatusCalculator.psStatus(liveData.psLeft.toFloat())
            val psRStatus = StatusCalculator.psStatus(liveData.psRight.toFloat())
            views.setTextColor(R.id.ps_left, StatusCalculator.textColor(psLStatus))
            views.setTextColor(R.id.ps_right, StatusCalculator.textColor(psRStatus))

            // LARGE: TE/PS bars + Time in zone with visual bars
            if (currentLayoutSize == LayoutSize.LARGE) {
                // TE and PS progress bars (average of L/R)
                val teAvg = (liveData.teLeft + liveData.teRight) / 2
                val psAvg = (liveData.psLeft + liveData.psRight) / 2
                views.setProgressBar(R.id.te_bar, 100, teAvg, false)
                views.setProgressBar(R.id.ps_bar, 50, psAvg, false)

                // Time in zone text
                views.setTextViewText(R.id.zone_optimal, "${liveData.zoneOptimal}%")
                views.setTextViewText(R.id.zone_attention, "${liveData.zoneAttention}%")
                views.setTextViewText(R.id.zone_problem, "${liveData.zoneProblem}%")

                // Zone stacked bar - set progress based on zone percentages
                // Each bar is always 100% filled, but their weights are set in layout
                // We use progress to show the relative amount
                views.setProgressBar(R.id.zone_bar_optimal, 100, if (liveData.zoneOptimal > 0) 100 else 0, false)
                views.setProgressBar(R.id.zone_bar_attention, 100, if (liveData.zoneAttention > 0) 100 else 0, false)
                views.setProgressBar(R.id.zone_bar_problem, 100, if (liveData.zoneProblem > 0) 100 else 0, false)
            }
        }
    }
}
