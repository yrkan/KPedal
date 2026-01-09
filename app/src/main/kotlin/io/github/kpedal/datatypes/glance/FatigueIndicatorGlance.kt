package io.github.kpedal.datatypes.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import io.github.kpedal.KPedalExtension
import io.github.kpedal.data.SensorDisconnectAction
import io.github.kpedal.datatypes.BaseDataType
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.SensorStreamState
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
import kotlin.math.abs

/**
 * Fatigue Indicator DataType content using Glance.
 * Shows fatigue level based on metric degradation over time.
 *
 * Fatigue detection logic:
 * - Compares current metrics to ride averages
 * - Tracks trend indicators (worse = -1, stable = 0, better = +1)
 * - More negative trends = higher fatigue
 */
@Composable
fun FatigueIndicatorContent(
    metrics: PedalingMetrics,
    liveData: LiveRideData,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !liveData.hasData || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    // Calculate fatigue level
    val fatigueData = if (!noData) calculateFatigue(metrics, liveData) else FatigueData()

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Fatigue icon + level
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 16)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ValueText(fatigueData.icon, fatigueData.color, 18)
                            ValueText(fatigueData.shortLabel, fatigueData.color, 14, GlanceModifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Fatigue icon + label
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 18)
                    } else {
                        ValueText(fatigueData.icon, fatigueData.color, 20)
                        ValueText(fatigueData.label, fatigueData.color, 16, GlanceModifier.padding(start = 6.dp))
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Fatigue icon + label - larger
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 20)
                    } else {
                        ValueText(fatigueData.icon, fatigueData.color, 24)
                        ValueText(fatigueData.label, fatigueData.color, 18, GlanceModifier.padding(start = 8.dp))
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Fatigue level + trend arrows
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Status section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            ValueText(fatigueData.icon, fatigueData.color, 24)
                            ValueText(fatigueData.label, fatigueData.color, 14)
                        }
                    }

                    GlanceDivider()

                    // Trend arrows
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            // Balance trend
                            val balArrow = getTrendArrow(liveData.balanceTrend)
                            val balColor = getTrendColor(liveData.balanceTrend)
                            ValueText(balArrow, balColor, 16)
                            LabelText("B", GlanceModifier.padding(start = 2.dp, end = 8.dp))

                            // TE trend
                            val teArrow = getTrendArrow(liveData.teTrend)
                            val teColor = getTrendColor(liveData.teTrend)
                            ValueText(teArrow, teColor, 16)
                            LabelText("T", GlanceModifier.padding(start = 2.dp, end = 8.dp))

                            // PS trend
                            val psArrow = getTrendArrow(liveData.psTrend)
                            val psColor = getTrendColor(liveData.psTrend)
                            ValueText(psArrow, psColor, 16)
                            LabelText("P", GlanceModifier.padding(start = 2.dp))
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // Fatigue level + detailed breakdown
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Main status
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("FATIGUE", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 24)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(fatigueData.icon, fatigueData.color, 28)
                                ValueText(fatigueData.label, fatigueData.color, 18, GlanceModifier.padding(start = 6.dp))
                            }
                        }
                    }

                    GlanceDivider()

                    // Delta from average
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("VS AVERAGE", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            val deltaText = formatDelta(fatigueData.efficiencyDelta)
                            val deltaColor = if (fatigueData.efficiencyDelta >= 0) GlanceColors.Optimal else GlanceColors.Attention
                            ValueText(deltaText, deltaColor, 20)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full breakdown
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Main status
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("FATIGUE LEVEL", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 24)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(fatigueData.icon, fatigueData.color, 28)
                                ValueText(fatigueData.label, fatigueData.color, 18, GlanceModifier.padding(start = 8.dp))
                            }
                        }
                    }

                    GlanceDivider()

                    // Metric trends
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Balance delta
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BAL", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                val arrow = getTrendArrow(liveData.balanceTrend)
                                val color = getTrendColor(liveData.balanceTrend)
                                ValueText(arrow, color, 18)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // TE delta
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                val arrow = getTrendArrow(liveData.teTrend)
                                val color = getTrendColor(liveData.teTrend)
                                ValueText(arrow, color, 18)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // PS delta
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                val arrow = getTrendArrow(liveData.psTrend)
                                val color = getTrendColor(liveData.psTrend)
                                ValueText(arrow, color, 18)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fatigue level data.
 */
private data class FatigueData(
    val level: Int = 0,           // 0 = Fresh, 1 = OK, 2 = Tired, 3 = Exhausted
    val icon: String = "?",
    val label: String = "?",
    val shortLabel: String = "?",
    val color: Color = GlanceColors.Label,
    val efficiencyDelta: Float = 0f
)

/**
 * Calculate fatigue level based on trends and deltas.
 */
private fun calculateFatigue(metrics: PedalingMetrics, liveData: LiveRideData): FatigueData {
    // Sum of trends: positive = improving, negative = degrading
    val trendSum = liveData.balanceTrend + liveData.teTrend + liveData.psTrend

    // Calculate efficiency delta (current TE vs average TE)
    val currentTeAvg = (metrics.torqueEffLeft + metrics.torqueEffRight) / 2f
    val avgTeAvg = (liveData.teLeft + liveData.teRight) / 2f
    val efficiencyDelta = if (avgTeAvg > 0) currentTeAvg - avgTeAvg else 0f

    // Fatigue level based on trends and current vs average
    val fatigueLevel = when {
        trendSum >= 2 -> 0  // Fresh - improving
        trendSum >= 0 && efficiencyDelta >= -2 -> 1  // OK - stable
        trendSum >= -1 || efficiencyDelta >= -5 -> 2  // Tired - some degradation
        else -> 3  // Exhausted - significant degradation
    }

    return when (fatigueLevel) {
        0 -> FatigueData(0, "▲▲", "FRESH", "OK", GlanceColors.Optimal, efficiencyDelta)
        1 -> FatigueData(1, "▲", "OK", "OK", GlanceColors.White, efficiencyDelta)
        2 -> FatigueData(2, "▼", "TIRED", "LOW", GlanceColors.Attention, efficiencyDelta)
        else -> FatigueData(3, "▼▼", "FATIGUED", "LOW", GlanceColors.Problem, efficiencyDelta)
    }
}

/**
 * Get trend arrow based on trend value.
 */
private fun getTrendArrow(trend: Int): String {
    return when {
        trend > 0 -> "▲"
        trend < 0 -> "▼"
        else -> "●"
    }
}

/**
 * Get trend color based on trend value.
 */
private fun getTrendColor(trend: Int): Color {
    return when {
        trend > 0 -> GlanceColors.Optimal
        trend < 0 -> GlanceColors.Attention
        else -> GlanceColors.White
    }
}

/**
 * Format delta value with sign.
 */
private fun formatDelta(delta: Float): String {
    val rounded = delta.toInt()
    return when {
        rounded > 0 -> "+$rounded"
        rounded < 0 -> "$rounded"
        else -> "0"
    }
}

/**
 * Fatigue Indicator DataType using Glance.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class FatigueIndicatorGlanceDataType(
    private val kpedalExtension: KPedalExtension
) : DataTypeImpl("kpedal", "fatigue-indicator") {

    companion object {
        private const val TAG = "FatigueIndicatorGlance"
        private const val VIEW_UPDATE_INTERVAL_MS = 1000L

        val PREVIEW_METRICS = PedalingMetrics(
            balance = 51f,
            torqueEffLeft = 70f,
            torqueEffRight = 72f,
            pedalSmoothLeft = 21f,
            pedalSmoothRight = 23f,
            timestamp = System.currentTimeMillis()
        )

        val PREVIEW_LIVE_DATA = LiveRideData(
            balanceLeft = 49,
            balanceRight = 51,
            teLeft = 72,
            teRight = 74,
            psLeft = 22,
            psRight = 24,
            balanceTrend = 0,
            teTrend = -1,
            psTrend = 0,
            hasData = true
        )
    }

    private val glance = GlanceRemoteViews()

    override fun startStream(emitter: Emitter<StreamState>) {
        emitter.onNext(StreamState.Streaming(
            DataPoint(dataTypeId = dataTypeId, values = emptyMap())
        ))
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        val layoutSize = BaseDataType.getLayoutSize(config)
        android.util.Log.d(TAG, "Grid: ${config.gridSize}, View: ${config.viewSize}, Size: $layoutSize")

        // Preview mode
        if (config.preview) {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    val result = glance.compose(context, DpSize.Unspecified) {
                        FatigueIndicatorContent(PREVIEW_METRICS, PREVIEW_LIVE_DATA, config, sensorDisconnected = false)
                    }
                    emitter.updateView(result.remoteViews)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Preview error: ${e.message}", e)
                }
            }
            emitter.setCancellable { }
            return
        }

        // Live mode
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        var disconnectAction = SensorDisconnectAction.SHOW_DASHES

        scope.launch {
            try {
                kpedalExtension.preferencesRepository.alertSettingsFlow.collect { settings ->
                    disconnectAction = settings.sensorDisconnectAction
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to collect alert settings: ${e.message}")
            }
        }

        // Initial render
        scope.launch {
            try {
                val metrics = kpedalExtension.pedalingEngine.metrics.value
                val liveData = kpedalExtension.pedalingEngine.liveDataCollector.liveData.value
                val result = glance.compose(context, DpSize.Unspecified) {
                    FatigueIndicatorContent(metrics, liveData, config, sensorDisconnected = false)
                }
                emitter.updateView(result.remoteViews)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Initial render failed: ${e.message}", e)
            }
        }

        // Fixed-rate updates
        scope.launch {
            var nextUpdateTime = System.currentTimeMillis() + VIEW_UPDATE_INTERVAL_MS
            while (isActive) {
                val now = System.currentTimeMillis()
                val delayMs = nextUpdateTime - now
                if (delayMs > 0) delay(delayMs)

                nextUpdateTime += VIEW_UPDATE_INTERVAL_MS

                val currentTime = System.currentTimeMillis()
                if (nextUpdateTime < currentTime) {
                    nextUpdateTime = currentTime + VIEW_UPDATE_INTERVAL_MS
                }

                try {
                    val sensorState = kpedalExtension.pedalingEngine.sensorState.value
                    val sensorDisconnected = sensorState is SensorStreamState.Disconnected &&
                            disconnectAction != SensorDisconnectAction.DISABLED

                    val metrics = kpedalExtension.pedalingEngine.metrics.value
                    val liveData = kpedalExtension.pedalingEngine.liveDataCollector.liveData.value

                    val result = glance.compose(context, DpSize.Unspecified) {
                        FatigueIndicatorContent(metrics, liveData, config, sensorDisconnected)
                    }
                    emitter.updateView(result.remoteViews)
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    android.util.Log.w(TAG, "Update error: ${t.javaClass.simpleName}: ${t.message}")
                }
            }
        }

        emitter.setCancellable {
            scope.cancel()
        }
    }
}
