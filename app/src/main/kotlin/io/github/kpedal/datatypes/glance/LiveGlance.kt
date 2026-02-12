package io.github.kpedal.datatypes.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import io.github.kpedal.KPedalExtension
import io.github.kpedal.data.SensorDisconnectAction
import io.github.kpedal.datatypes.BaseDataType
import io.github.kpedal.engine.SensorStreamState
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
 * Live DataType content using Glance.
 * Shows aggregated ride stats with Time In Zone.
 */
@Composable
fun LiveContent(
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
                // Optimal zone % with status bar - compact
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 20)
                    } else {
                        val statusColor = getZoneStatusColor(liveData.zoneOptimal, liveData.zoneAttention, liveData.zoneProblem)
                        ValueText("${liveData.zoneOptimal}%", statusColor, 20)
                    }

                    // Status bar
                    if (noData) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 4.dp)
                                .background(GlanceColors.Label)
                        ) {}
                    } else {
                        ZoneStatusBar(
                            liveData.zoneOptimal,
                            liveData.zoneAttention,
                            liveData.zoneProblem,
                            GlanceModifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Optimal zone % with status bar
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 22)
                    } else {
                        val statusColor = getZoneStatusColor(liveData.zoneOptimal, liveData.zoneAttention, liveData.zoneProblem)
                        ValueText("${liveData.zoneOptimal}%", statusColor, 22)
                    }

                    // Status bar
                    if (noData) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 4.dp)
                                .background(GlanceColors.Label)
                        ) {}
                    } else {
                        ZoneStatusBar(
                            liveData.zoneOptimal,
                            liveData.zoneAttention,
                            liveData.zoneProblem,
                            GlanceModifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Optimal zone % with status bar - larger
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 24)
                    } else {
                        val statusColor = getZoneStatusColor(liveData.zoneOptimal, liveData.zoneAttention, liveData.zoneProblem)
                        ValueText("${liveData.zoneOptimal}%", statusColor, 24)
                    }

                    // Status bar
                    if (noData) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .padding(top = 4.dp)
                                .background(GlanceColors.Label)
                        ) {}
                    } else {
                        ZoneStatusBar(
                            liveData.zoneOptimal,
                            liveData.zoneAttention,
                            liveData.zoneProblem,
                            GlanceModifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Balance + TE/PS averages
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BAL", GlanceModifier.padding(end = 4.dp), fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                val balanceStatus = StatusCalculator.balanceStatus(liveData.balanceRight.toFloat())
                                val (leftColor, rightColor) = if (balanceStatus != StatusCalculator.Status.OPTIMAL) {
                                    val statusColor = getStatusColor(balanceStatus)
                                    if (liveData.balanceLeft > 52) {
                                        Pair(statusColor, GlanceColors.White)
                                    } else {
                                        Pair(GlanceColors.White, statusColor)
                                    }
                                } else {
                                    Pair(GlanceColors.White, GlanceColors.White)
                                }
                                ValueText("${liveData.balanceLeft}", leftColor, 22)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText("${liveData.balanceRight}", rightColor, 22)
                            }
                        }
                    }

                    GlanceDivider()

                    // TE and PS side by side (averages only)
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // TE section
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 20)
                            } else {
                                val teAvg = (liveData.teLeft + liveData.teRight) / 2
                                ValueText("$teAvg", getTEColor(teAvg.toFloat()), 20)
                            }
                        }

                        // PS section
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 20)
                            } else {
                                val psAvg = (liveData.psLeft + liveData.psRight) / 2
                                ValueText("$psAvg", getPSColor(psAvg.toFloat()), 20)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // NARROW: Balance + TE/PS with larger fonts
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 28)
                        } else {
                            val balanceStatus = StatusCalculator.balanceStatus(liveData.balanceRight.toFloat())
                            val (leftColor, rightColor) = if (balanceStatus != StatusCalculator.Status.OPTIMAL) {
                                val statusColor = getStatusColor(balanceStatus)
                                if (liveData.balanceLeft > 52) {
                                    Pair(statusColor, GlanceColors.White)
                                } else {
                                    Pair(GlanceColors.White, statusColor)
                                }
                            } else {
                                Pair(GlanceColors.White, GlanceColors.White)
                            }
                            BalanceRow(
                                "${liveData.balanceLeft}",
                                "${liveData.balanceRight}",
                                leftColor, rightColor,
                                valueFontSize = 28
                            )
                        }
                    }

                    GlanceDivider()

                    // TE section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("TORQUE EFF", fontSize = 12)
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 24)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText(displayText, GlanceColors.Label, 24)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${liveData.teLeft}", getTEColor(liveData.teLeft.toFloat()), 24)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText("${liveData.teRight}", getTEColor(liveData.teRight.toFloat()), 24)
                            }
                        }
                    }

                    GlanceDivider()

                    // PS section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PEDAL SMOOTH", fontSize = 12)
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 24)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText(displayText, GlanceColors.Label, 24)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${liveData.psLeft}", getPSColor(liveData.psLeft.toFloat()), 24)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText("${liveData.psRight}", getPSColor(liveData.psRight.toFloat()), 24)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Balance + TE/PS + Time in Zone
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 28)
                        } else {
                            val balanceStatus = StatusCalculator.balanceStatus(liveData.balanceRight.toFloat())
                            val (leftColor, rightColor) = if (balanceStatus != StatusCalculator.Status.OPTIMAL) {
                                val statusColor = getStatusColor(balanceStatus)
                                if (liveData.balanceLeft > 52) {
                                    Pair(statusColor, GlanceColors.White)
                                } else {
                                    Pair(GlanceColors.White, statusColor)
                                }
                            } else {
                                Pair(GlanceColors.White, GlanceColors.White)
                            }
                            BalanceRow(
                                "${liveData.balanceLeft}",
                                "${liveData.balanceRight}",
                                leftColor, rightColor,
                                valueFontSize = 28
                            )
                        }
                    }

                    GlanceDivider()

                    // TE and PS row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // TE section
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 12)
                            if (noData) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText(displayText, GlanceColors.Label, 22)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 4.dp))
                                    ValueText(displayText, GlanceColors.Label, 22)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText("${liveData.teLeft}", getTEColor(liveData.teLeft.toFloat()), 22)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 4.dp))
                                    ValueText("${liveData.teRight}", getTEColor(liveData.teRight.toFloat()), 22)
                                }
                            }
                        }

                        // PS section
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 12)
                            if (noData) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText(displayText, GlanceColors.Label, 22)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 4.dp))
                                    ValueText(displayText, GlanceColors.Label, 22)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText("${liveData.psLeft}", getPSColor(liveData.psLeft.toFloat()), 22)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 4.dp))
                                    ValueText("${liveData.psRight}", getPSColor(liveData.psRight.toFloat()), 22)
                                }
                            }
                        }
                    }

                    GlanceDivider()

                    // Time in Zone section - professional layout
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("TIME IN ZONE", fontSize = 11)

                        if (noData) {
                            // No data state
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .padding(top = 6.dp)
                                    .background(GlanceColors.Divider)
                            ) {}
                        } else {
                            // Segmented progress bar
                            ZoneSegmentedBar(
                                liveData.zoneOptimal,
                                liveData.zoneAttention,
                                liveData.zoneProblem,
                                height = 10,
                                modifier = GlanceModifier.padding(top = 6.dp)
                            )

                            // Zone percentages below bar
                            Row(
                                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Optimal
                                Box(
                                    modifier = GlanceModifier.defaultWeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZoneChip(liveData.zoneOptimal, GlanceColors.Optimal)
                                }
                                // Attention
                                Box(
                                    modifier = GlanceModifier.defaultWeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZoneChip(liveData.zoneAttention, GlanceColors.Attention)
                                }
                                // Problem
                                Box(
                                    modifier = GlanceModifier.defaultWeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZoneChip(liveData.zoneProblem, GlanceColors.Problem)
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
 * Live DataType using Glance.
 * This DataType is special because it uses LiveRideData instead of PedalingMetrics.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class LiveGlanceDataType(
    private val kpedalExtension: KPedalExtension
) : DataTypeImpl("kpedal", "live") {

    companion object {
        private const val TAG = "LiveGlanceDataType"
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
            android.util.Log.d(TAG, "Preview mode")
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    val result = glance.compose(context, DpSize.Unspecified) {
                        LiveContent(PREVIEW_LIVE_DATA, config, sensorDisconnected = false)
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

        // Track disconnect action preference
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
                    LiveContent(liveData, config, sensorDisconnected = false)
                }
                emitter.updateView(result.remoteViews)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Initial render failed: ${e.message}", e)
            }
        }

        // Fixed-rate updates at 1Hz (more accurate than fixed delay)
        scope.launch {
            var nextUpdateTime = System.currentTimeMillis() + VIEW_UPDATE_INTERVAL_MS
            while (isActive) {
                // Wait until next scheduled update
                val now = System.currentTimeMillis()
                val delayMs = nextUpdateTime - now
                if (delayMs > 0) delay(delayMs)

                // Schedule next update (fixed rate, not fixed delay)
                nextUpdateTime += VIEW_UPDATE_INTERVAL_MS

                // Prevent drift if we fell behind (e.g., device was sleeping)
                val currentTime = System.currentTimeMillis()
                if (nextUpdateTime < currentTime) {
                    nextUpdateTime = currentTime + VIEW_UPDATE_INTERVAL_MS
                }

                try {
                    val sensorState = kpedalExtension.pedalingEngine.sensorState.value
                    val sensorDisconnected = sensorState is SensorStreamState.Disconnected &&
                            disconnectAction != SensorDisconnectAction.DISABLED

                    val liveData = kpedalExtension.pedalingEngine.liveDataCollector.liveData.value

                    // Glance.compose() creates NEW RemoteViews - no action accumulation!
                    val result = glance.compose(context, DpSize.Unspecified) {
                        LiveContent(liveData, config, sensorDisconnected)
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
