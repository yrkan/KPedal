package io.github.kpedal.datatypes.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
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
 * Power Focus DataType content using Glance.
 * Power as PRIMARY metric with pedaling quality as secondary.
 * For power-focused training with technique monitoring.
 */
@Composable
fun PowerFocusContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    val power = metrics.power
    val teAvg = ((metrics.torqueEffLeft + metrics.torqueEffRight) / 2).toInt()
    val psAvg = ((metrics.pedalSmoothLeft + metrics.pedalSmoothRight) / 2).toInt()

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Power only - large
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ValueText("$power", GlanceColors.White, 24)
                    LabelText("W")
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Power + Balance indicator
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 22)
                            LabelText("w", GlanceModifier.padding(start = 1.dp, bottom = 3.dp))
                        }
                    }
                    // Balance
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 14)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 14)
                                ValueText(":", GlanceColors.Separator, 10)
                                ValueText("${metrics.balance.toInt()}", rightColor, 14)
                            }
                        }
                        LabelText("BAL")
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Power + Balance + TE
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PWR", fontSize = 11)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 22)
                            LabelText("w", GlanceModifier.padding(start = 1.dp, bottom = 3.dp), fontSize = 10)
                        }
                    }
                    // Balance
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BAL", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 16)
                                ValueText(":", GlanceColors.Separator, 11)
                                ValueText("${metrics.balance.toInt()}", rightColor, 16)
                            }
                        }
                    }
                    // TE
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("TE", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            ValueText("$teAvg", getTEColor(teAvg.toFloat()), 18)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Power (big, top) + BAL/TE row (bottom)
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Power section - emphasized
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("POWER", fontSize = 11)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 30)
                            LabelText("W", GlanceModifier.padding(start = 2.dp, bottom = 4.dp), fontSize = 11)
                        }
                    }

                    GlanceDivider()

                    // BAL and TE row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Balance
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BAL", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                val (leftColor, rightColor) = getBalanceColors(metrics)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 14)
                                    ValueText(":", GlanceColors.Separator, 10)
                                    ValueText("${metrics.balance.toInt()}", rightColor, 14)
                                }
                            }
                        }
                        // TE
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                ValueText("$teAvg", getTEColor(teAvg.toFloat()), 16)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // Power + Balance + TE + PS
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Power section - prominent
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("POWER", fontSize = 12)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 32)
                            LabelText("W", GlanceModifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 12)
                        }
                    }

                    GlanceDivider()

                    // Balance section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 18)
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

                    GlanceDivider()

                    // TE and PS row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("$teAvg", getTEColor(teAvg.toFloat()), 18)
                            }
                        }

                        GlanceVerticalDivider()

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("$psAvg", getPSColor(psAvg.toFloat()), 18)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Power dashboard with all pedaling metrics
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LabelText("POWER FOCUS", fontSize = 12)
                    }

                    GlanceDivider()

                    // Power section - main focus
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 34)
                            LabelText("W", GlanceModifier.padding(start = 4.dp, bottom = 6.dp), fontSize = 14)
                        }
                    }

                    GlanceDivider()

                    // Pedaling metrics row
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
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                val (leftColor, rightColor) = getBalanceColors(metrics)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 16)
                                    ValueText(":", GlanceColors.Separator, 11)
                                    ValueText("${metrics.balance.toInt()}", rightColor, 16)
                                }
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // TE
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                ValueText("$teAvg", getTEColor(teAvg.toFloat()), 18)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // PS
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 14)
                            } else {
                                ValueText("$psAvg", getPSColor(psAvg.toFloat()), 18)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Power Focus DataType using Glance.
 */
class PowerFocusGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "power-focus") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        PowerFocusContent(metrics, config, sensorDisconnected)
    }
}
