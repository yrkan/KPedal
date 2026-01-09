package io.github.kpedal.datatypes.glance

import androidx.compose.runtime.Composable
import io.github.kpedal.KPedalExtension
import io.github.kpedal.datatypes.GlanceDataType
import io.github.kpedal.engine.PedalingMetrics
import io.hammerhead.karooext.models.ViewConfig

/**
 * Full Overview DataType using Glance.
 *
 * Shows Balance + TE + PS all in one compact view.
 * Uses GlanceRemoteViews which creates fresh RemoteViews on each update,
 * preventing the action accumulation crash that occurred with the old approach.
 */
class FullOverviewGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "full-overview") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        FullOverviewContent(metrics, config, sensorDisconnected)
    }
}
