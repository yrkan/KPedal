package io.github.kpedal.datatypes

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.data.SensorDisconnectAction
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.SensorStreamState
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
 * Base class for Glance-based DataTypes.
 *
 * Uses Jetpack Glance (GlanceRemoteViews) which creates a NEW RemoteViews on each compose() call.
 * This automatically solves the RemoteViews action accumulation problem that caused crashes
 * after 10-15 minutes of riding with the old approach.
 *
 * Old approach: Single RemoteViews instance accumulated 900 updates × 25 actions = 22,500 actions
 * New approach: Fresh RemoteViews on each update, max ~25 actions per instance
 *
 * Based on official Hammerhead sample: CustomSpeedDataType.kt
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class GlanceDataType(
    protected val kpedalExtension: KPedalExtension,
    typeId: String
) : DataTypeImpl("kpedal", typeId) {

    companion object {
        private const val TAG = "GlanceDataType"

        /**
         * Minimum interval between view updates in milliseconds.
         * Karoo SDK drops updates faster than 1Hz.
         */
        private const val VIEW_UPDATE_INTERVAL_MS = 1000L

        /**
         * Sample metrics for preview mode.
         */
        val PREVIEW_METRICS = PedalingMetrics(
            balance = 51f,
            torqueEffLeft = 74f,
            torqueEffRight = 76f,
            pedalSmoothLeft = 22f,
            pedalSmoothRight = 24f,
            power = 245,
            cadence = 90,
            balance3s = 50f,
            balance10s = 50f,
            timestamp = System.currentTimeMillis()
        )
    }

    private val glance = GlanceRemoteViews()

    /**
     * Render the DataType content using Glance composables.
     *
     * @param metrics Current pedaling metrics
     * @param config View configuration (size, alignment, etc.)
     * @param sensorDisconnected Whether the sensor is currently disconnected
     */
    @Composable
    protected abstract fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    )

    override fun startStream(emitter: Emitter<StreamState>) {
        emitter.onNext(StreamState.Streaming(
            DataPoint(dataTypeId = dataTypeId, values = emptyMap())
        ))
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        // Hide default Karoo header
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        val layoutSize = BaseDataType.getLayoutSize(config)
        android.util.Log.d(TAG, "[$dataTypeId] Grid: ${config.gridSize}, View: ${config.viewSize}, Size: $layoutSize")

        // Preview mode: render immediately with sample data
        if (config.preview) {
            android.util.Log.d(TAG, "[$dataTypeId] Preview mode")
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    val result = glance.compose(context, DpSize.Unspecified) {
                        Content(PREVIEW_METRICS, config, sensorDisconnected = false)
                    }
                    emitter.updateView(result.remoteViews)
                    android.util.Log.d(TAG, "[$dataTypeId] Preview render successful")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "[$dataTypeId] Preview error: ${e.message}", e)
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
                android.util.Log.w(TAG, "[$dataTypeId] Failed to collect alert settings: ${e.message}")
            }
        }

        // Initial render
        scope.launch {
            try {
                val initialMetrics = kpedalExtension.pedalingEngine.metrics.value
                val result = glance.compose(context, DpSize.Unspecified) {
                    Content(initialMetrics, config, sensorDisconnected = false)
                }
                emitter.updateView(result.remoteViews)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[$dataTypeId] Initial render failed: ${e.message}", e)
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
                    val currentSensorState = kpedalExtension.pedalingEngine.sensorState.value
                    val sensorDisconnected = currentSensorState is SensorStreamState.Disconnected &&
                            disconnectAction != SensorDisconnectAction.DISABLED

                    val metrics = kpedalExtension.pedalingEngine.metrics.value

                    // Glance.compose() creates a NEW RemoteViews each time - no action accumulation!
                    val result = glance.compose(context, DpSize.Unspecified) {
                        Content(metrics, config, sensorDisconnected)
                    }
                    emitter.updateView(result.remoteViews)
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    android.util.Log.w(TAG, "[$dataTypeId] Update error: ${t.javaClass.simpleName}: ${t.message}")
                }
            }
        }

        emitter.setCancellable {
            scope.cancel()
        }
    }
}
