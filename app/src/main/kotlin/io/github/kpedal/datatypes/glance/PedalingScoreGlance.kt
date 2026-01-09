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

/**
 * Pedaling Score DataType content using Glance.
 * Shows overall pedaling quality score (0-100) with breakdown.
 */
@Composable
fun PedalingScoreContent(
    liveData: LiveRideData,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !liveData.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Score only - compact
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 18)
                    } else {
                        val scoreColor = getScoreColor(liveData.score)
                        ValueText("${liveData.score}", scoreColor, 24)
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Score with label
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("SCORE", GlanceModifier.padding(end = 8.dp))
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 20)
                    } else {
                        val scoreColor = getScoreColor(liveData.score)
                        ValueText("${liveData.score}", scoreColor, 24)
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Score with label - larger
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("SCORE", GlanceModifier.padding(end = 10.dp), fontSize = 11)
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 22)
                    } else {
                        val scoreColor = getScoreColor(liveData.score)
                        ValueText("${liveData.score}", scoreColor, 28)
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Score + zone optimal %
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Score section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("SCORE", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 24)
                        } else {
                            val scoreColor = getScoreColor(liveData.score)
                            ValueText("${liveData.score}", scoreColor, 28)
                        }
                    }

                    GlanceDivider()

                    // Zone optimal section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("OPTIMAL", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            ValueText("${liveData.zoneOptimal}%", GlanceColors.Optimal, 20)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // Score + Balance + TE breakdown
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Score section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("SCORE", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 28)
                        } else {
                            val scoreColor = getScoreColor(liveData.score)
                            ValueText("${liveData.score}", scoreColor, 32)
                        }
                    }

                    GlanceDivider()

                    // Breakdown: Balance | TE
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
                                val balanceAvg = (liveData.balanceLeft + liveData.balanceRight) / 2
                                val imbalance = kotlin.math.abs(50 - balanceAvg)
                                val balanceScore = when {
                                    imbalance <= 2 -> 100
                                    imbalance <= 5 -> 80
                                    imbalance <= 8 -> 60
                                    else -> 40
                                }
                                ValueText("$balanceScore", getScoreColor(balanceScore), 20)
                            }
                        }

                        GlanceVerticalDivider()

                        // TE
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("EFF", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val teAvg = (liveData.teLeft + liveData.teRight) / 2
                                val teScore = when {
                                    teAvg >= 70 -> 100
                                    teAvg >= 60 -> 80
                                    teAvg >= 50 -> 60
                                    else -> 40
                                }
                                ValueText("$teScore", getScoreColor(teScore), 20)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Score + all breakdowns
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Score section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PEDALING SCORE", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 28)
                        } else {
                            val scoreColor = getScoreColor(liveData.score)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("${liveData.score}", scoreColor, 32)
                                LabelText("/100", GlanceModifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 12)
                            }
                        }
                    }

                    GlanceDivider()

                    // Breakdown row: Balance | TE | PS
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Balance score
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BAL", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                val imbalance = kotlin.math.abs(liveData.balanceLeft - liveData.balanceRight)
                                val balanceScore = when {
                                    imbalance <= 4 -> 100
                                    imbalance <= 10 -> 80
                                    imbalance <= 16 -> 60
                                    else -> 40
                                }
                                ValueText("$balanceScore", getScoreColor(balanceScore), 18)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // TE score
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("EFF", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                val teAvg = (liveData.teLeft + liveData.teRight) / 2
                                val teScore = when {
                                    teAvg >= 70 -> 100
                                    teAvg >= 60 -> 80
                                    teAvg >= 50 -> 60
                                    else -> 40
                                }
                                ValueText("$teScore", getScoreColor(teScore), 18)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // Zone score (consistency)
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("ZONE", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("${liveData.zoneOptimal}", getScoreColor(liveData.zoneOptimal), 18)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get color based on score value (0-100).
 */
private fun getScoreColor(score: Int): Color {
    return when {
        score >= 80 -> GlanceColors.Optimal
        score >= 60 -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
}

/**
 * Pedaling Score DataType using Glance.
 * Uses LiveRideData for aggregated score.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class PedalingScoreGlanceDataType(
    private val kpedalExtension: KPedalExtension
) : DataTypeImpl("kpedal", "pedaling-score") {

    companion object {
        private const val TAG = "PedalingScoreGlance"
        private const val VIEW_UPDATE_INTERVAL_MS = 1000L

        val PREVIEW_LIVE_DATA = LiveRideData(
            score = 78,
            balanceLeft = 49,
            balanceRight = 51,
            teLeft = 72,
            teRight = 74,
            psLeft = 22,
            psRight = 24,
            zoneOptimal = 72,
            zoneAttention = 20,
            zoneProblem = 8,
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
                        PedalingScoreContent(PREVIEW_LIVE_DATA, config, sensorDisconnected = false)
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
                val liveData = try {
                    kpedalExtension.pedalingEngine.liveDataCollector.liveData.value
                } catch (e: Exception) {
                    PREVIEW_LIVE_DATA
                }
                val result = glance.compose(context, DpSize.Unspecified) {
                    PedalingScoreContent(liveData, config, sensorDisconnected = false)
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

                    val liveData = kpedalExtension.pedalingEngine.liveDataCollector.liveData.value

                    val result = glance.compose(context, DpSize.Unspecified) {
                        PedalingScoreContent(liveData, config, sensorDisconnected)
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
