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
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Cadence + Balance DataType content using Glance.
 * Shows cadence as primary metric with balance as secondary.
 * Useful for maintaining pedaling rhythm while monitoring balance.
 */
@Composable
fun CadenceBalanceContent(
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
                // Cadence + Balance compact
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cadence
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ValueText("${metrics.cadence}", GlanceColors.White, 24)
                    }
                    // Balance
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
                                ValueText(":", GlanceColors.Separator, 14)
                                ValueText("${metrics.balance.toInt()}", rightColor, 20)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Cadence + Balance horizontal
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cadence
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.cadence}", GlanceColors.White, 20)
                            LabelText("rpm", GlanceModifier.padding(start = 2.dp, bottom = 2.dp))
                        }
                    }
                    // Balance
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 16)
                                ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 2.dp))
                                ValueText("${metrics.balance.toInt()}", rightColor, 16)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Cadence + Balance horizontal larger
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cadence
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.cadence}", GlanceColors.White, 24)
                            LabelText("rpm", GlanceModifier.padding(start = 2.dp, bottom = 3.dp), fontSize = 11)
                        }
                    }
                    // Balance
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 18)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText("${metrics.balance.toInt()}", rightColor, 18)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Cadence + Balance stacked
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Cadence section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("CADENCE", fontSize = 11)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.cadence}", GlanceColors.White, 24)
                            LabelText("rpm", GlanceModifier.padding(start = 2.dp, bottom = 3.dp), fontSize = 11)
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
                                valueFontSize = 18
                            )
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // Cadence + Balance + Cadence zone
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Cadence section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("CADENCE", fontSize = 12)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.cadence}", getCadenceColor(metrics.cadence), 28)
                            LabelText("rpm", GlanceModifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 12)
                        }
                    }

                    GlanceDivider()

                    // Balance section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 22)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            BalanceRow(
                                "${metrics.balanceLeft.toInt()}",
                                "${metrics.balance.toInt()}",
                                leftColor, rightColor,
                                valueFontSize = 22
                            )
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full layout with labels
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Cadence section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("CADENCE", fontSize = 12)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.cadence}", getCadenceColor(metrics.cadence), 34)
                            LabelText("rpm", GlanceModifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 12)
                        }
                    }

                    GlanceDivider()

                    // Balance section with L/R labels
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
                                    ValueText(displayText, GlanceColors.Label, 26)
                                } else {
                                    val (leftColor, _) = getBalanceColors(metrics)
                                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 26)
                                }
                            }
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LabelText("R", fontSize = 12)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 26)
                                } else {
                                    val (_, rightColor) = getBalanceColors(metrics)
                                    ValueText("${metrics.balance.toInt()}", rightColor, 26)
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
 * Get cadence color based on value (optimal range: 80-100 RPM).
 */
private fun getCadenceColor(cadence: Int): androidx.compose.ui.graphics.Color {
    return when {
        cadence in 80..100 -> GlanceColors.Optimal
        cadence in 70..110 -> GlanceColors.White
        cadence in 60..120 -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
}

/**
 * Cadence + Balance DataType using Glance.
 */
class CadenceBalanceGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "cadence-balance") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        CadenceBalanceContent(metrics, config, sensorDisconnected)
    }
}
