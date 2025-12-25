package io.github.kpedal.datatypes

import android.content.Context
import android.widget.RemoteViews
import io.github.kpedal.KPedalExtension
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
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
 * Base class for all kpedal DataTypes to reduce code duplication.
 * Handles common lifecycle management and view setup.
 *
 * IMPORTANT: ViewEmitter.updateView() can only be called at 1Hz (once per second).
 * Updates more frequent than this are dropped by the Karoo SDK.
 * This class implements rate limiting to respect this constraint.
 *
 * Supports adaptive layouts based on ViewConfig.gridSize:
 * - SMALL: height < 20 rows (less than 1/3 of screen)
 * - MEDIUM: height 20-40 rows (1/3 to 2/3 of screen)
 * - LARGE: height > 40 rows (more than 2/3 of screen)
 */
abstract class BaseDataType(
    protected val kpedalExtension: KPedalExtension,
    typeId: String
) : DataTypeImpl("kpedal", typeId) {

    /**
     * Size category for adaptive layouts.
     * Based on Karoo's 60-unit grid system.
     */
    enum class LayoutSize {
        SMALL,   // < 20 rows - minimal info, large text
        MEDIUM,  // 20-40 rows - standard layout
        LARGE    // > 40 rows - full detail
    }

    companion object {
        private const val TAG = "BaseDataType"
        /**
         * Minimum interval between view updates in milliseconds.
         * Karoo SDK drops updates faster than 1Hz, so we limit to ~1 update per second.
         */
        private const val VIEW_UPDATE_INTERVAL_MS = 1000L

        /**
         * Sample metrics for preview mode in the Profiles editor.
         * Shows realistic-looking data so user can see how the widget will appear.
         * Note: balanceLeft, torqueEffAvg, pedalSmoothAvg are computed properties.
         */
        val PREVIEW_METRICS = PedalingMetrics(
            balance = 51f,           // Right side - balanceLeft will be 49
            torqueEffLeft = 74f,
            torqueEffRight = 76f,    // torqueEffAvg will be 75
            pedalSmoothLeft = 22f,
            pedalSmoothRight = 24f,  // pedalSmoothAvg will be 23
            power = 245,
            cadence = 90,
            balance3s = 50f,
            balance10s = 50f,
            timestamp = System.currentTimeMillis()
        )

        /**
         * Determine layout size category from ViewConfig.
         * Grid is 60 units total, so:
         * - rows < 20 = less than 1/3 height = SMALL
         * - rows 20-40 = 1/3 to 2/3 height = MEDIUM
         * - rows > 40 = more than 2/3 height = LARGE
         */
        fun getLayoutSize(config: ViewConfig): LayoutSize {
            val rows = config.gridSize.second
            return when {
                rows < 20 -> LayoutSize.SMALL
                rows <= 40 -> LayoutSize.MEDIUM
                else -> LayoutSize.LARGE
            }
        }
    }

    private var viewScope: CoroutineScope? = null
    private var isPreviewMode: Boolean = false
    protected var currentLayoutSize: LayoutSize = LayoutSize.MEDIUM

    /**
     * Get the layout resource ID for this DataType based on size.
     * Subclasses should return different layouts for different sizes.
     *
     * @param size The calculated layout size (SMALL, MEDIUM, LARGE)
     * @return Layout resource ID
     */
    protected abstract fun getLayoutResId(size: LayoutSize): Int

    /**
     * Update the RemoteViews with current metrics.
     * Called at most once per second (1Hz) to comply with Karoo SDK limits.
     *
     * @param views The RemoteViews to update
     * @param metrics Current pedaling metrics
     * @param config View configuration with grid size and other settings
     */
    protected abstract fun updateViews(views: RemoteViews, metrics: PedalingMetrics)

    /**
     * Optional: Called once when view is created. Override to customize initial setup.
     * @param views The RemoteViews to configure
     * @param config View configuration containing gridSize, viewSize, textSize, etc.
     */
    protected open fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        // Default: do nothing. Subclasses can override to adapt layout based on config.
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        emitter.onNext(StreamState.Streaming(
            DataPoint(dataTypeId = dataTypeId, values = emptyMap())
        ))
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        viewScope?.cancel()
        viewScope = null

        // Track preview mode for this view instance
        isPreviewMode = config.preview

        // Hide default Karoo header - we render our own UI
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        // Determine layout size and select appropriate layout
        currentLayoutSize = getLayoutSize(config)
        android.util.Log.d(TAG, "[$dataTypeId] Grid: ${config.gridSize}, Size: $currentLayoutSize")

        val cachedViews = RemoteViews(context.packageName, getLayoutResId(currentLayoutSize))

        // Allow subclasses to customize initial view setup based on config
        onViewCreated(cachedViews, config)

        // Preview mode: render immediately with sample data, no coroutines needed
        if (isPreviewMode) {
            android.util.Log.d(TAG, "[$dataTypeId] Preview mode, rendering with PREVIEW_METRICS")
            try {
                updateViews(cachedViews, PREVIEW_METRICS)
                emitter.updateView(cachedViews)
                android.util.Log.d(TAG, "[$dataTypeId] Preview render successful")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[$dataTypeId] Preview update error: ${e.message}", e)
            }
            // No update loop needed for preview - it's static
            emitter.setCancellable { isPreviewMode = false }
            return
        }

        // Not preview mode - still try to render something immediately
        android.util.Log.d(TAG, "[$dataTypeId] Live mode, config.preview=${config.preview}")

        // Render PREVIEW_METRICS first to ensure something shows immediately
        try {
            updateViews(cachedViews, PREVIEW_METRICS)
            emitter.updateView(cachedViews)
            android.util.Log.d(TAG, "[$dataTypeId] Initial preview render successful")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "[$dataTypeId] Initial preview render failed: ${e.message}", e)
        }

        // Live mode: use coroutines for async updates
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        viewScope = scope

        // Start live updates after a short delay (let initial render complete)
        scope.launch {
            delay(500L) // Give initial render time to complete

            try {
                val initialMetrics = try {
                    kpedalExtension.pedalingEngine.metrics.value
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "[$dataTypeId] Engine not ready: ${e.message}")
                    return@launch // Keep showing preview metrics
                }
                updateViews(cachedViews, initialMetrics)
                emitter.updateView(cachedViews)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "[$dataTypeId] Live update error: ${e.message}")
            }
        }

        // Rate-limited updates at 1Hz
        scope.launch {
            try {
                while (isActive) {
                    delay(VIEW_UPDATE_INTERVAL_MS)

                    // Get latest metrics (skip if engine not ready)
                    val metrics = try {
                        kpedalExtension.pedalingEngine.metrics.value
                    } catch (e: Exception) {
                        continue // Skip this update cycle
                    }
                    updateViews(cachedViews, metrics)
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
            isPreviewMode = false
        }
    }

    /**
     * Helper to apply balance colors to left/right TextViews.
     * @param views RemoteViews to update
     * @param leftResId Resource ID of left balance TextView
     * @param rightResId Resource ID of right balance TextView
     * @param left Left balance value (0-100)
     * @param right Right balance value (0-100)
     */
    protected fun applyBalanceColors(
        views: RemoteViews,
        leftResId: Int,
        rightResId: Int,
        left: Int,
        right: Int
    ) {
        val balanceStatus = StatusCalculator.balanceStatus(right.toFloat())

        if (balanceStatus != StatusCalculator.Status.OPTIMAL) {
            val statusColor = StatusCalculator.statusColor(balanceStatus)
            if (left > 52) {
                views.setTextColor(leftResId, statusColor)
                views.setTextColor(rightResId, StatusCalculator.COLOR_WHITE)
            } else {
                views.setTextColor(leftResId, StatusCalculator.COLOR_WHITE)
                views.setTextColor(rightResId, statusColor)
            }
        } else {
            views.setTextColor(leftResId, StatusCalculator.COLOR_WHITE)
            views.setTextColor(rightResId, StatusCalculator.COLOR_WHITE)
        }
    }
}
