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
 * Power + Balance DataType content using Glance.
 * Shows large power value with balance values.
 */
@Composable
fun PowerBalanceContent(
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
                // PWR + BAL horizontal - power is primary
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power (larger, primary metric)
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ValueText("${metrics.power}", GlanceColors.White, 24)
                    }
                    // Balance (secondary)
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 20)
                                ValueText("/", GlanceColors.Separator, 14)
                                ValueText("${metrics.balance.toInt()}", rightColor, 20)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Power + Balance horizontal
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.power}", GlanceColors.White, 22)
                            LabelText("w", GlanceModifier.padding(start = 1.dp, bottom = 3.dp))
                        }
                    }
                    // Balance
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 20)
                                ValueText("/", GlanceColors.Separator, 14)
                                ValueText(displayText, GlanceColors.Label, 20)
                            }
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 20)
                                ValueText("/", GlanceColors.Separator, 14)
                                ValueText("${metrics.balance.toInt()}", rightColor, 20)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Power + Balance horizontal with larger values
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.power}", GlanceColors.White, 24)
                            LabelText("W", GlanceModifier.padding(start = 2.dp, bottom = 2.dp), fontSize = 11)
                        }
                    }
                    // Balance
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 20)
                                ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText(displayText, GlanceColors.Label, 20)
                            }
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 20)
                                ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText("${metrics.balance.toInt()}", rightColor, 20)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Power + Balance stacked with labels
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Power section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PWR", fontSize = 11)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.power}", GlanceColors.White, 24)
                            LabelText("W", GlanceModifier.padding(start = 2.dp, bottom = 2.dp), fontSize = 11)
                        }
                    }

                    GlanceDivider()

                    // Balance section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 20)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            BalanceRow(
                                "${metrics.balanceLeft.toInt()}",
                                "${metrics.balance.toInt()}",
                                leftColor, rightColor,
                                valueFontSize = 20
                            )
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // NARROW: more vertical space - larger values
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Power section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("POWER", fontSize = 12)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.power}", GlanceColors.White, 34)
                            LabelText("W", GlanceModifier.padding(start = 2.dp, bottom = 4.dp), fontSize = 12)
                        }
                    }

                    GlanceDivider()

                    // Balance section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BALANCE", fontSize = 12)
                            if (noData) {
                                BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 28)
                            } else {
                                val (leftColor, rightColor) = getBalanceColors(metrics)
                                BalanceRow(
                                    "${metrics.balanceLeft.toInt()}",
                                    "${metrics.balance.toInt()}",
                                    leftColor, rightColor,
                                    valueFontSize = 28
                                )
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full layout
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Power section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("POWER", fontSize = 12)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.power}", GlanceColors.White, 38)
                            LabelText("W", GlanceModifier.padding(start = 4.dp, bottom = 6.dp), fontSize = 14)
                        }
                    }

                    GlanceDivider()

                    // Balance section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BALANCE", fontSize = 12)
                            if (noData) {
                                BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 28)
                            } else {
                                val (leftColor, rightColor) = getBalanceColors(metrics)
                                BalanceRow(
                                    "${metrics.balanceLeft.toInt()}",
                                    "${metrics.balance.toInt()}",
                                    leftColor, rightColor,
                                    valueFontSize = 28
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Power Balance DataType using Glance.
 */
class PowerBalanceGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "power-balance") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        PowerBalanceContent(metrics, config, sensorDisconnected)
    }
}
