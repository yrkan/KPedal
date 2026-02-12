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
 * Left Leg Focus DataType content using Glance.
 * Shows all metrics for the left leg: Balance %, TE, PS.
 * Useful for cyclists focusing on left leg technique.
 */
@Composable
fun LeftLegContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    val balanceLeft = metrics.balanceLeft.toInt()
    val teLeft = metrics.torqueEffLeft.toInt()
    val psLeft = metrics.pedalSmoothLeft.toInt()

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
                        ValueText(displayText, GlanceColors.Label, 22)
                    } else {
                        val color = getLeftBalanceColor(balanceLeft)
                        ValueText("$balanceLeft%", color, 26)
                    }
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
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            val color = getLeftBalanceColor(balanceLeft)
                            ValueText("$balanceLeft%", color, 22)
                        }
                    }
                    // TE
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            ValueText("$teLeft", getTEColor(teLeft.toFloat()), 22)
                        }
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
                            val color = getLeftBalanceColor(balanceLeft)
                            ValueText("$balanceLeft%", color, 20)
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
                            ValueText("$teLeft", getTEColor(teLeft.toFloat()), 20)
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
                            ValueText("$psLeft", getPSColor(psLeft.toFloat()), 20)
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
                        LabelText("L BAL", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 22)
                        } else {
                            val color = getLeftBalanceColor(balanceLeft)
                            ValueText("$balanceLeft%", color, 24)
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
                                ValueText("$teLeft", getTEColor(teLeft.toFloat()), 18)
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
                                ValueText("$psLeft", getPSColor(psLeft.toFloat()), 18)
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
                    LabelText("LEFT LEG", GlanceModifier.padding(top = 4.dp), fontSize = 12)

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
                            val color = getLeftBalanceColor(balanceLeft)
                            ValueText("$balanceLeft%", color, 28)
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
                            ValueText("$teLeft", getTEColor(teLeft.toFloat()), 22)
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
                            ValueText("$psLeft", getPSColor(psLeft.toFloat()), 22)
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
                        LabelText("LEFT LEG", fontSize = 12)
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
                            ValueText(displayText, GlanceColors.Label, 32)
                        } else {
                            val color = getLeftBalanceColor(balanceLeft)
                            ValueText("$balanceLeft%", color, 34)
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
                                ValueText(displayText, GlanceColors.Label, 26)
                            } else {
                                ValueText("$teLeft", getTEColor(teLeft.toFloat()), 28)
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
                                ValueText(displayText, GlanceColors.Label, 26)
                            } else {
                                ValueText("$psLeft", getPSColor(psLeft.toFloat()), 28)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get color for left leg balance.
 * Optimal range: 48-52% (balanced).
 */
private fun getLeftBalanceColor(balanceLeft: Int): Color {
    return when {
        balanceLeft in 48..52 -> GlanceColors.Optimal
        balanceLeft in 45..55 -> GlanceColors.White
        balanceLeft in 40..60 -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
}

/**
 * Left Leg Focus DataType using Glance.
 */
class LeftLegGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "left-leg") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        LeftLegContent(metrics, config, sensorDisconnected)
    }
}
