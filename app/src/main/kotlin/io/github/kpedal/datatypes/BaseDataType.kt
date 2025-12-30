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
     * Based on gridSize (60 units total) and viewSize (pixels).
     *
     * Karoo 3 screen: 480x800px, grid: 60x60 units
     *
     * Full width (gridSize.first >= 50):
     * - 1 field:     (60,60) 470x790 → LARGE
     * - 2 fields:    (60,30) 470x390 → LARGE
     * - 3 fields:    (60,20) 470x260 → MEDIUM
     * - 4 fields:    (60,15) 470x195 → MEDIUM_WIDE (2-row wide)
     * - 5 fields 2:1:(60,24) 470x320 → MEDIUM (large fields)
     * - 5 fields 2:1:(60,12) 470x160 → SMALL_WIDE (small fields)
     * - 6 fields:    (60,10) 470x130 → SMALL_WIDE (1-row wide)
     *
     * Half width (gridSize.first < 50):
     * - 2 horiz:  (30,60) 235x790 → NARROW (tall, narrow)
     * - 2x2 grid: (30,30) 235x390 → MEDIUM
     * - 2x3 grid: (30,20) 235x260 → MEDIUM
     * - 2x4 grid: (30,15) 235x195 → SMALL
     * - 2x5 grid: (30,12) 235x156 → SMALL
     * - 2x6 grid: (30,10) 235x130 → SMALL
     */
    enum class LayoutSize {
        SMALL,       // Short height, narrow width - minimal content
        SMALL_WIDE,  // 1-row wide, full width, very short (~130-160px) - horizontal compact
        MEDIUM_WIDE, // 2-row wide, full width, short (~160-220px) - more space than SMALL_WIDE
        MEDIUM,      // 3-row wide or half width medium - balanced layout (~220-350px)
        LARGE,       // Large height, full width - detailed layout (350px+)
        NARROW       // Tall height, narrow width (2 fields side by side)
    }

    companion object {
        private const val TAG = "BaseDataType"
        /**
         * Minimum interval between view updates in milliseconds.
         * Karoo SDK drops updates faster than 1Hz, so we limit to ~1 update per second.
         */
        private const val VIEW_UPDATE_INTERVAL_MS = 1000L

        /**
         * Text to display when no data is available.
         * Standard cycling computer convention.
         */
        const val NO_DATA = "-"

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
         * Uses gridSize.first to detect full width (60) vs half width (30).
         *
         * Karoo 3 screen: 480x800px, grid: 60x60 units
         *
         * Full width (gridSize.first >= 50):
         * - 1 field:     (60,60) 470x790 → LARGE
         * - 2 fields:    (60,30) 470x390 → LARGE
         * - 3 fields:    (60,20) 470x260 → MEDIUM
         * - 4 fields:    (60,15) 470x195 → MEDIUM_WIDE
         * - 5 fields 2:1:(60,24) 470x320 → MEDIUM (large fields)
         * - 5 fields 2:1:(60,12) 470x160 → SMALL_WIDE (small fields)
         * - 6 fields:    (60,10) 470x130 → SMALL_WIDE
         *
         * Half width (gridSize.first < 50):
         * - 2 horiz:  (30,60) 235x790 → NARROW (tall, narrow)
         * - 2x2 grid: (30,30) 235x390 → MEDIUM
         * - 2x3 grid: (30,20) 235x260 → MEDIUM
         * - 2x4 grid: (30,15) 235x195 → SMALL
         * - 2x5 grid: (30,12) 235x156 → SMALL
         * - 2x6 grid: (30,10) 235x130 → SMALL
         */
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

        /**
         * Determine aspect ratio for layout orientation decisions.
         */
        fun getAspectRatio(config: ViewConfig): AspectRatio {
            val width = config.viewSize.first.toFloat()
            val height = config.viewSize.second.toFloat()
            val ratio = width / height

            return when {
                ratio > 1.5f -> AspectRatio.WIDE
                ratio < 0.67f -> AspectRatio.TALL
                else -> AspectRatio.SQUARE
            }
        }
    }

    private var viewScope: CoroutineScope? = null
    private var isPreviewMode: Boolean = false
    protected var currentLayoutSize: LayoutSize = LayoutSize.MEDIUM
    protected var currentAspectRatio: AspectRatio = AspectRatio.SQUARE
    protected var currentConfig: ViewConfig? = null

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
     * Called once when view is created. Override to set adaptive text sizes.
     *
     * Use TextSizeCalculator to compute sizes based on config.textSize:
     * ```
     * override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
     *     views.setAdaptiveTextSize(R.id.balance_left, config, TextSizeCalculator.Role.PRIMARY)
     *     views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)
     * }
     * ```
     *
     * @param views The RemoteViews to configure
     * @param config View configuration containing gridSize, viewSize, textSize, etc.
     */
    protected open fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        // Default: do nothing. Subclasses override to set adaptive text sizes.
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

        // Store config and determine layout size and aspect ratio
        currentConfig = config
        currentLayoutSize = getLayoutSize(config)
        currentAspectRatio = getAspectRatio(config)
        android.util.Log.d(TAG, "[$dataTypeId] Grid: ${config.gridSize}, View: ${config.viewSize}, TextSize: ${config.textSize}, Size: $currentLayoutSize, Aspect: $currentAspectRatio")

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

        // Not preview mode - render real data (or "-" if no data)
        android.util.Log.d(TAG, "[$dataTypeId] Live mode, config.preview=${config.preview}")

        // Render current metrics immediately (shows "-" if no data yet)
        val initialMetrics = try {
            kpedalExtension.pedalingEngine.metrics.value
        } catch (e: Exception) {
            android.util.Log.w(TAG, "[$dataTypeId] Engine not ready: ${e.message}")
            PedalingMetrics() // Default metrics with hasData = false (shows "-")
        }
        try {
            updateViews(cachedViews, initialMetrics)
            emitter.updateView(cachedViews)
            android.util.Log.d(TAG, "[$dataTypeId] Initial live render successful (hasData=${initialMetrics.hasData})")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "[$dataTypeId] Initial live render failed: ${e.message}", e)
        }

        // Live mode: use coroutines for async updates
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        viewScope = scope

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
            currentConfig = null
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
