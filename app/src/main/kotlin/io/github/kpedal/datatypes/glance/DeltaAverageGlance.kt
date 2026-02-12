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
 * Delta vs Average DataType content using Glance.
 * Shows current values compared to ride averages.
 * Positive delta = better than average, negative = worse.
 */
@Composable
fun DeltaAverageContent(
    metrics: PedalingMetrics,
    liveData: LiveRideData,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !liveData.hasData || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    // Calculate deltas
    val balanceDelta = if (!noData) calculateBalanceDelta(metrics, liveData) else 0f
    val teDelta = if (!noData) calculateTEDelta(metrics, liveData) else 0f
    val psDelta = if (!noData) calculatePSDelta(metrics, liveData) else 0f

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Overall delta indicator
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 18)
                    } else {
                        val avgDelta = (teDelta + psDelta) / 2
                        val (text, color) = formatDeltaWithColor(avgDelta)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ValueText(text, color, 24)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // TE and PS deltas
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TE delta
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            val (text, color) = formatDeltaWithColor(teDelta)
                            ValueText(text, color, 22)
                        }
                    }
                    // PS delta
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            val (text, color) = formatDeltaWithColor(psDelta)
                            ValueText(text, color, 22)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // TE and PS deltas - larger
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TE delta
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            val (text, color) = formatDeltaWithColor(teDelta)
                            ValueText(text, color, 26)
                        }
                    }
                    // PS delta
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            val (text, color) = formatDeltaWithColor(psDelta)
                            ValueText(text, color, 26)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Balance delta + TE/PS deltas
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance delta
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BAL", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            // For balance, closer to 0 is better (less imbalance delta)
                            val (text, color) = formatBalanceDeltaWithColor(balanceDelta)
                            ValueText(text, color, 28)
                        }
                    }

                    GlanceDivider()

                    // TE and PS deltas
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val (text, color) = formatDeltaWithColor(teDelta)
                                ValueText(text, color, 24)
                            }
                        }
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val (text, color) = formatDeltaWithColor(psDelta)
                                ValueText(text, color, 24)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // All deltas stacked
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance delta
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BAL vs AVG", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 24)
                        } else {
                            val (text, color) = formatBalanceDeltaWithColor(balanceDelta)
                            ValueText(text, color, 30)
                        }
                    }

                    GlanceDivider()

                    // TE delta
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("TE vs AVG", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            val (text, color) = formatDeltaWithColor(teDelta)
                            ValueText(text, color, 26)
                        }
                    }

                    GlanceDivider()

                    // PS delta
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PS vs AVG", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            val (text, color) = formatDeltaWithColor(psDelta)
                            ValueText(text, color, 26)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // All deltas with current + average display
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("DELTA vs AVERAGE", fontSize = 12)
                    }

                    GlanceDivider()

                    // Delta values row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Balance
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BAL", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val (text, color) = formatBalanceDeltaWithColor(balanceDelta)
                                ValueText(text, color, 26)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // TE
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val (text, color) = formatDeltaWithColor(teDelta)
                                ValueText(text, color, 26)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // PS
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val (text, color) = formatDeltaWithColor(psDelta)
                                ValueText(text, color, 26)
                            }
                        }
                    }

                    GlanceDivider()

                    // Current vs Average summary
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            val currentTE = ((metrics.torqueEffLeft + metrics.torqueEffRight) / 2).toInt()
                            val avgTE = (liveData.teLeft + liveData.teRight) / 2
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LabelText("NOW ", fontSize = 11)
                                    ValueText("$currentTE", GlanceColors.White, 22)
                                    LabelText(" | AVG ", fontSize = 11)
                                    ValueText("$avgTE", GlanceColors.Label, 22)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate balance delta (current imbalance vs average imbalance).
 * Negative means more imbalanced than average.
 */
private fun calculateBalanceDelta(metrics: PedalingMetrics, liveData: LiveRideData): Float {
    val currentImbalance = abs(50 - metrics.balance)
    val avgImbalance = abs(50 - liveData.balanceRight)
    return avgImbalance - currentImbalance  // Positive = less imbalance now (better)
}

/**
 * Calculate TE delta (current TE vs average TE).
 */
private fun calculateTEDelta(metrics: PedalingMetrics, liveData: LiveRideData): Float {
    val currentTE = (metrics.torqueEffLeft + metrics.torqueEffRight) / 2
    val avgTE = (liveData.teLeft + liveData.teRight) / 2f
    return currentTE - avgTE
}

/**
 * Calculate PS delta (current PS vs average PS).
 */
private fun calculatePSDelta(metrics: PedalingMetrics, liveData: LiveRideData): Float {
    val currentPS = (metrics.pedalSmoothLeft + metrics.pedalSmoothRight) / 2
    val avgPS = (liveData.psLeft + liveData.psRight) / 2f
    return currentPS - avgPS
}

/**
 * Format delta value with color (positive = green, negative = yellow/red).
 */
private fun formatDeltaWithColor(delta: Float): Pair<String, Color> {
    val rounded = delta.toInt()
    val text = when {
        rounded > 0 -> "+$rounded"
        rounded < 0 -> "$rounded"
        else -> "0"
    }
    val color = when {
        rounded >= 3 -> GlanceColors.Optimal
        rounded >= 0 -> GlanceColors.White
        rounded >= -3 -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
    return text to color
}

/**
 * Format balance delta with color (closer to 0 = better balance now).
 */
private fun formatBalanceDeltaWithColor(delta: Float): Pair<String, Color> {
    val rounded = delta.toInt()
    val text = when {
        rounded > 0 -> "+$rounded"
        rounded < 0 -> "$rounded"
        else -> "0"
    }
    // For balance: positive delta means we're MORE balanced now than average
    val color = when {
        rounded >= 2 -> GlanceColors.Optimal
        rounded >= 0 -> GlanceColors.White
        rounded >= -2 -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
    return text to color
}

/**
 * Delta vs Average DataType using Glance.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class DeltaAverageGlanceDataType(
    private val kpedalExtension: KPedalExtension
) : DataTypeImpl("kpedal", "delta-average") {

    companion object {
        private const val TAG = "DeltaAverageGlance"
        private const val VIEW_UPDATE_INTERVAL_MS = 1000L

        val PREVIEW_METRICS = PedalingMetrics(
            balance = 52f,
            torqueEffLeft = 74f,
            torqueEffRight = 76f,
            pedalSmoothLeft = 24f,
            pedalSmoothRight = 26f,
            timestamp = System.currentTimeMillis()
        )

        val PREVIEW_LIVE_DATA = LiveRideData(
            balanceLeft = 49,
            balanceRight = 51,
            teLeft = 72,
            teRight = 74,
            psLeft = 22,
            psRight = 24,
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
                        DeltaAverageContent(PREVIEW_METRICS, PREVIEW_LIVE_DATA, config, sensorDisconnected = false)
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
                    DeltaAverageContent(metrics, liveData, config, sensorDisconnected = false)
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
                        DeltaAverageContent(metrics, liveData, config, sensorDisconnected)
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
