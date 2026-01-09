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
import io.hammerhead.karooext.models.ViewConfig

/**
 * Single Balance DataType content using Glance.
 * Full screen balance display with large numbers.
 */
@Composable
fun SingleBalanceContent(
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
                // Compact: large values only (context is clear from field name)
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (noData) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ValueText(displayText, GlanceColors.Label, 20)
                            ValueText("/", GlanceColors.Separator, 16)
                            ValueText(displayText, GlanceColors.Label, 20)
                        }
                    } else {
                        val (leftColor, rightColor) = getBalanceColors(metrics)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ValueText("${metrics.balanceLeft.toInt()}", leftColor, 20)
                            ValueText("/", GlanceColors.Separator, 16)
                            ValueText("${metrics.balance.toInt()}", rightColor, 20)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Horizontal: BAL label + L | R
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("BAL", GlanceModifier.padding(end = 8.dp))
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 22)
                        ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 4.dp))
                        ValueText(displayText, GlanceColors.Label, 22)
                    } else {
                        val (leftColor, rightColor) = getBalanceColors(metrics)
                        ValueText("${metrics.balanceLeft.toInt()}", leftColor, 22)
                        ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 4.dp))
                        ValueText("${metrics.balance.toInt()}", rightColor, 22)
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Horizontal with larger values
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("BAL", GlanceModifier.padding(end = 8.dp), fontSize = 11)
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 24)
                        ValueText("|", GlanceColors.Separator, 18, GlanceModifier.padding(horizontal = 6.dp))
                        ValueText(displayText, GlanceColors.Label, 24)
                    } else {
                        val (leftColor, rightColor) = getBalanceColors(metrics)
                        ValueText("${metrics.balanceLeft.toInt()}", leftColor, 24)
                        ValueText("|", GlanceColors.Separator, 18, GlanceModifier.padding(horizontal = 6.dp))
                        ValueText("${metrics.balance.toInt()}", rightColor, 24)
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Medium: header + L/R labels + values
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("BAL", fontSize = 11)
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Left
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LabelText("L", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 24)
                            } else {
                                val (leftColor, _) = getBalanceColors(metrics)
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 24)
                            }
                        }
                        // Right
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LabelText("R", fontSize = 11)
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
            BaseDataType.LayoutSize.NARROW -> {
                // Narrow: more vertical space - larger values
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("BALANCE", fontSize = 12)
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Left
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LabelText("L", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 28)
                            } else {
                                val (leftColor, _) = getBalanceColors(metrics)
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 28)
                            }
                        }
                        // Right
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LabelText("R", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 28)
                            } else {
                                val (_, rightColor) = getBalanceColors(metrics)
                                ValueText("${metrics.balance.toInt()}", rightColor, 28)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Large: full layout with header and large values
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("BALANCE", fontSize = 12)
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Left
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("LEFT", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 32)
                            } else {
                                val (leftColor, _) = getBalanceColors(metrics)
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 32)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp), height = 28)

                        // Right
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("RIGHT", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 32)
                            } else {
                                val (_, rightColor) = getBalanceColors(metrics)
                                ValueText("${metrics.balance.toInt()}", rightColor, 32)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Balance DataType using Glance.
 */
class SingleBalanceGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "single-balance") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        SingleBalanceContent(metrics, config, sensorDisconnected)
    }
}
