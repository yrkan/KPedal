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
import kotlinx.coroutines.launch

/**
 * Base class for all kpedal DataTypes to reduce code duplication.
 * Handles common lifecycle management and view setup.
 */
abstract class BaseDataType(
    protected val kpedalExtension: KPedalExtension,
    typeId: String
) : DataTypeImpl("kpedal", typeId) {

    private var viewScope: CoroutineScope? = null

    /**
     * Create the layout resource ID for this DataType.
     */
    protected abstract fun getLayoutResId(): Int

    /**
     * Update the RemoteViews with current metrics.
     * Called on each metrics update.
     */
    protected abstract fun updateViews(views: RemoteViews, metrics: PedalingMetrics)

    override fun startStream(emitter: Emitter<StreamState>) {
        emitter.onNext(StreamState.Streaming(
            DataPoint(dataTypeId = dataTypeId, values = emptyMap())
        ))
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        viewScope?.cancel()
        viewScope = null

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        viewScope = scope

        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        val cachedViews = RemoteViews(context.packageName, getLayoutResId())

        scope.launch {
            try {
                kpedalExtension.pedalingEngine.metrics.collect { metrics ->
                    updateViews(cachedViews, metrics)
                    emitter.updateView(cachedViews)
                }
            } catch (e: Exception) {
                // Log but don't crash - scope will be cancelled by setCancellable
                android.util.Log.w("BaseDataType", "Metrics collection error: ${e.message}")
            }
        }

        emitter.setCancellable {
            viewScope?.cancel()
            viewScope = null
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
