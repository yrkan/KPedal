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
 * Compact Multi DataType content using Glance.
 * Maximum information in minimum space.
 * BAL·TE·PS in the most compact format possible.
 */
@Composable
fun CompactMultiContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    val teAvg = ((metrics.torqueEffLeft + metrics.torqueEffRight) / 2).toInt()
    val psAvg = ((metrics.pedalSmoothLeft + metrics.pedalSmoothRight) / 2).toInt()

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // BAL only (most important for small)
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 22)
                    } else {
                        val (leftColor, rightColor) = getBalanceColors(metrics)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ValueText("${metrics.balanceLeft.toInt()}", leftColor, 22)
                            ValueText(":", GlanceColors.Separator, 12)
                            ValueText("${metrics.balance.toInt()}", rightColor, 22)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // BAL·TE compact
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BAL
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 22)
                                ValueText(":", GlanceColors.Separator, 14)
                                ValueText("${metrics.balance.toInt()}", rightColor, 22)
                            }
                        }
                    }
                    // TE
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            ValueText("$teAvg", getTEColor(teAvg.toFloat()), 22)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // BAL·TE·PS all in one row
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BAL
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 22)
                                ValueText(":", GlanceColors.Separator, 12)
                                ValueText("${metrics.balance.toInt()}", rightColor, 22)
                            }
                        }
                    }
                    // TE
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            ValueText("$teAvg", getTEColor(teAvg.toFloat()), 24)
                        }
                    }
                    // PS
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            ValueText("$psAvg", getPSColor(psAvg.toFloat()), 24)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // BAL (top) + TE/PS (bottom)
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 24)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            BalanceRow(
                                "${metrics.balanceLeft.toInt()}",
                                "${metrics.balance.toInt()}",
                                leftColor, rightColor,
                                valueFontSize = 26
                            )
                        }
                    }

                    GlanceDivider()

                    // TE + PS row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("$teAvg", getTEColor(teAvg.toFloat()), 24)
                            }
                        }
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("$psAvg", getPSColor(psAvg.toFloat()), 24)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // BAL + TE + PS + L/R TE detail
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance section with L/R labels
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("L / R BALANCE", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 28)
                                ValueText(" : ", GlanceColors.Separator, 16)
                                ValueText("${metrics.balance.toInt()}", rightColor, 28)
                            }
                        }
                    }

                    GlanceDivider()

                    // TE section with L/R
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE L", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.torqueEffLeft.toInt()}", getTEColor(metrics.torqueEffLeft), 24)
                            }
                        }

                        GlanceVerticalDivider()

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE R", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.torqueEffRight.toInt()}", getTEColor(metrics.torqueEffRight), 24)
                            }
                        }
                    }

                    GlanceDivider()

                    // PS section with L/R
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS L", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.pedalSmoothLeft.toInt()}", getPSColor(metrics.pedalSmoothLeft), 24)
                            }
                        }

                        GlanceVerticalDivider()

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS R", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.pedalSmoothRight.toInt()}", getPSColor(metrics.pedalSmoothRight), 24)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full compact dashboard with all metrics L/R
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance with visual bar concept (numbers)
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 30)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            BalanceRow(
                                "${metrics.balanceLeft.toInt()}",
                                "${metrics.balance.toInt()}",
                                leftColor, rightColor,
                                valueFontSize = 34
                            )
                        }
                    }

                    GlanceDivider()

                    // TE L/R row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE L", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.torqueEffLeft.toInt()}", getTEColor(metrics.torqueEffLeft), 26)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TE R", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.torqueEffRight.toInt()}", getTEColor(metrics.torqueEffRight), 26)
                            }
                        }
                    }

                    GlanceDivider()

                    // PS L/R row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS L", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.pedalSmoothLeft.toInt()}", getPSColor(metrics.pedalSmoothLeft), 26)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS R", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                ValueText("${metrics.pedalSmoothRight.toInt()}", getPSColor(metrics.pedalSmoothRight), 26)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Multi DataType using Glance.
 */
class CompactMultiGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "compact-multi") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        CompactMultiContent(metrics, config, sensorDisconnected)
    }
}
