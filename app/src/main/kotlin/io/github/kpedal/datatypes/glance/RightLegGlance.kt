package io.github.kpedal.datatypes.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
 * Right Leg Focus DataType content using Glance.
 * Shows all metrics for the right leg: Balance %, TE, PS.
 * Useful for cyclists focusing on right leg technique.
 */
@Composable
fun RightLegContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    val balanceRight = metrics.balance.toInt()
    val teRight = metrics.torqueEffRight.toInt()
    val psRight = metrics.pedalSmoothRight.toInt()

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Balance % only
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 18)
                    } else {
                        val color = getRightBalanceColor(balanceRight)
                        ValueText("$balanceRight%", color, 20)
                    }
                    LabelText("R")
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Balance + TE
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balance
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            val color = getRightBalanceColor(balanceRight)
                            ValueText("$balanceRight%", color, 18)
                        }
                        LabelText("BAL")
                    }
                    // TE
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            ValueText("$teRight", getTEColor(teRight.toFloat()), 18)
                        }
                        LabelText("TE")
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Balance + TE + PS
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balance
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            val color = getRightBalanceColor(balanceRight)
                            ValueText("$balanceRight%", color, 20)
                        }
                        LabelText("BAL", fontSize = 11)
                    }
                    // TE
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            ValueText("$teRight", getTEColor(teRight.toFloat()), 20)
                        }
                        LabelText("TE", fontSize = 11)
                    }
                    // PS
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            ValueText("$psRight", getPSColor(psRight.toFloat()), 20)
                        }
                        LabelText("PS", fontSize = 11)
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Balance + TE/PS stacked
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Balance
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("R BAL", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            val color = getRightBalanceColor(balanceRight)
                            ValueText("$balanceRight%", color, 24)
                        }
                    }

                    GlanceDivider()

                    // TE and PS
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
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("$teRight", getTEColor(teRight.toFloat()), 18)
                            }
                        }
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("$psRight", getPSColor(psRight.toFloat()), 18)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // All metrics stacked with labels
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header
                    LabelText("RIGHT LEG", GlanceModifier.padding(top = 4.dp), fontSize = 12)

                    // Balance
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 26)
                        } else {
                            val color = getRightBalanceColor(balanceRight)
                            ValueText("$balanceRight%", color, 28)
                        }
                    }

                    GlanceDivider()

                    // TE
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("TORQUE EFF", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            ValueText("$teRight", getTEColor(teRight.toFloat()), 22)
                        }
                    }

                    GlanceDivider()

                    // PS
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PEDAL SMOOTH", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            ValueText("$psRight", getPSColor(psRight.toFloat()), 22)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full layout with all metrics
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LabelText("RIGHT LEG", fontSize = 12)
                    }

                    GlanceDivider()

                    // Balance
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 26)
                        } else {
                            val color = getRightBalanceColor(balanceRight)
                            ValueText("$balanceRight%", color, 28)
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
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                ValueText("$teRight", getTEColor(teRight.toFloat()), 20)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PS", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                ValueText("$psRight", getPSColor(psRight.toFloat()), 20)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get color for right leg balance.
 * Optimal range: 48-52% (balanced).
 */
private fun getRightBalanceColor(balanceRight: Int): Color {
    return when {
        balanceRight in 48..52 -> GlanceColors.Optimal
        balanceRight in 45..55 -> GlanceColors.White
        balanceRight in 40..60 -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
}

/**
 * Right Leg Focus DataType using Glance.
 */
class RightLegGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "right-leg") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        RightLegContent(metrics, config, sensorDisconnected)
    }
}
