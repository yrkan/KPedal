package io.github.kpedal.datatypes.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import io.github.kpedal.KPedalExtension
import io.github.kpedal.datatypes.BaseDataType
import io.github.kpedal.datatypes.GlanceDataType
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Quick Glance DataType content using Glance.
 * Shows status indicator (checkmark or warning) + Balance.
 */
@Composable
fun QuickGlanceContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Status icon + text - compact
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText("!", GlanceColors.Attention, 20)
                        ValueText(displayText, GlanceColors.Label, 16, GlanceModifier.padding(start = 4.dp))
                    } else {
                        val (icon, iconColor, statusText, statusColor) = getStatusInfo(metrics)
                        ValueText(icon, iconColor, 20)
                        ValueText(statusText, statusColor, 14, GlanceModifier.padding(start = 4.dp))
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Status icon + text
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText("!", GlanceColors.Attention, 22)
                        ValueText(displayText, GlanceColors.Label, 18, GlanceModifier.padding(start = 6.dp))
                    } else {
                        val (icon, iconColor, statusText, statusColor) = getStatusInfo(metrics)
                        ValueText(icon, iconColor, 22)
                        ValueText(statusText, statusColor, 16, GlanceModifier.padding(start = 6.dp))
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Status icon + text - larger
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText("!", GlanceColors.Attention, 24)
                        ValueText(displayText, GlanceColors.Label, 20, GlanceModifier.padding(start = 8.dp))
                    } else {
                        val (icon, iconColor, statusText, statusColor) = getStatusInfo(metrics)
                        ValueText(icon, iconColor, 24)
                        ValueText(statusText, statusColor, 18, GlanceModifier.padding(start = 8.dp))
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Status + Balance
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status section
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText("!", GlanceColors.Attention, 24)
                            ValueText(displayText, GlanceColors.Label, 16, GlanceModifier.padding(start = 4.dp))
                        } else {
                            val (icon, iconColor, statusText, statusColor) = getStatusInfo(metrics)
                            ValueText(icon, iconColor, 24)
                            ValueText(statusText, statusColor, 14, GlanceModifier.padding(start = 4.dp))
                        }
                    }

                    GlanceDivider()

                    // Balance section
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                            ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 4.dp))
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            ValueText("${metrics.balanceLeft.toInt()}", leftColor, 20)
                            ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 4.dp))
                            ValueText("${metrics.balance.toInt()}", rightColor, 20)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // NARROW: Status + Balance with larger fonts
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status section
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText("!", GlanceColors.Attention, 28)
                            ValueText(displayText, GlanceColors.Label, 18, GlanceModifier.padding(start = 6.dp))
                        } else {
                            val (icon, iconColor, statusText, statusColor) = getStatusInfo(metrics)
                            ValueText(icon, iconColor, 28)
                            ValueText(statusText, statusColor, 16, GlanceModifier.padding(start = 6.dp))
                        }
                    }

                    GlanceDivider()

                    // Balance section with labels
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LabelText("L", fontSize = 12)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 24)
                                } else {
                                    val (leftColor, _) = getBalanceColors(metrics)
                                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 24)
                                }
                            }
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LabelText("R", fontSize = 12)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 24)
                                } else {
                                    val (_, rightColor) = getBalanceColors(metrics)
                                    ValueText("${metrics.balance.toInt()}", rightColor, 24)
                                }
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full layout with labels
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText("!", GlanceColors.Attention, 28)
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            val (icon, iconColor, statusText, statusColor) = getStatusInfo(metrics)
                            ValueText(icon, iconColor, 28)
                            ValueText(statusText, statusColor, 14)
                        }
                    }

                    GlanceDivider()

                    // Balance section with labels
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LabelText("L", fontSize = 12)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 22)
                                } else {
                                    val (leftColor, _) = getBalanceColors(metrics)
                                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 22)
                                }
                            }
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LabelText("R", fontSize = 12)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 22)
                                } else {
                                    val (_, rightColor) = getBalanceColors(metrics)
                                    ValueText("${metrics.balance.toInt()}", rightColor, 22)
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
 * Get status info based on metrics.
 */
private fun getStatusInfo(metrics: PedalingMetrics): StatusInfo {
    val balanceOk = StatusCalculator.balanceStatus(metrics.balance) == StatusCalculator.Status.OPTIMAL
    val teOk = StatusCalculator.teStatus(metrics.torqueEffAvg) == StatusCalculator.Status.OPTIMAL
    val psOk = StatusCalculator.psStatus(metrics.pedalSmoothAvg) == StatusCalculator.Status.OPTIMAL

    val issues = mutableListOf<String>()
    if (!balanceOk) issues.add("Bal")
    if (!teOk) issues.add("TE")
    if (!psOk) issues.add("PS")

    return if (issues.isEmpty()) {
        StatusInfo("✓", GlanceColors.Optimal, "OK", GlanceColors.Optimal)
    } else {
        val color = if (issues.size >= 2) GlanceColors.Problem else GlanceColors.Attention
        StatusInfo("!", color, issues.joinToString(", "), color)
    }
}

private data class StatusInfo(
    val icon: String,
    val iconColor: androidx.compose.ui.graphics.Color,
    val text: String,
    val textColor: androidx.compose.ui.graphics.Color
)

/**
 * Quick Glance DataType using Glance.
 */
class QuickGlanceGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "quick-glance") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        QuickGlanceContent(metrics, config, sensorDisconnected)
    }
}
