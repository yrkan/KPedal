package io.github.kpedal.datatypes.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
 * Balance Trend DataType content using Glance.
 * Shows current balance + 3s and 10s smoothed values.
 */
@Composable
fun BalanceTrendContent(
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
                // Balance + trend arrow - compact
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balance section
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BAL")
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 16)
                                ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 2.dp))
                                ValueText(displayText, GlanceColors.Label, 16)
                            }
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 16)
                                ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 2.dp))
                                ValueText("${metrics.balance.toInt()}", rightColor, 16)
                            }
                        }
                    }

                    // Trend indicator
                    Box(
                        modifier = GlanceModifier.fillMaxHeight().padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 14)
                        } else {
                            val (arrow, color) = getTrendIndicator(metrics)
                            ValueText(arrow, color, 18)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Balance + trend arrow
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balance section
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BAL")
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 18)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 2.dp))
                                ValueText(displayText, GlanceColors.Label, 18)
                            }
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 18)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 2.dp))
                                ValueText("${metrics.balance.toInt()}", rightColor, 18)
                            }
                        }
                    }

                    // Trend indicator
                    Box(
                        modifier = GlanceModifier.fillMaxHeight().padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            val (arrow, color) = getTrendIndicator(metrics)
                            ValueText(arrow, color, 20)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Balance + trend arrow - larger
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balance section
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BAL", fontSize = 11)
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 20)
                                ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText(displayText, GlanceColors.Label, 20)
                            }
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 20)
                                ValueText("|", GlanceColors.Separator, 16, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText("${metrics.balance.toInt()}", rightColor, 20)
                            }
                        }
                    }

                    // Trend indicator
                    Box(
                        modifier = GlanceModifier.fillMaxHeight().padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            val (arrow, color) = getTrendIndicator(metrics)
                            ValueText(arrow, color, 22)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Current + 3s smoothed
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Current balance
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BAL", GlanceModifier.padding(end = 8.dp), fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 20)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText(displayText, GlanceColors.Label, 20)
                            } else {
                                val (leftColor, rightColor) = getBalanceColors(metrics)
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 20)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText("${metrics.balance.toInt()}", rightColor, 20)
                            }
                        }
                    }

                    GlanceDivider()

                    // 3s smoothed
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("3s", GlanceModifier.padding(end = 8.dp), fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                                ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("${metrics.balance3sLeft.toInt()}", GlanceColors.White, 16)
                                ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 4.dp))
                                ValueText("${metrics.balance3s.toInt()}", GlanceColors.White, 16)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // NARROW: Current + 3s + 10s smoothed - stacked vertically with larger fonts
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Current balance
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("BALANCE", fontSize = 12)
                        if (noData) {
                            BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label, valueFontSize = 24)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            BalanceRow(
                                "${metrics.balanceLeft.toInt()}",
                                "${metrics.balance.toInt()}",
                                leftColor, rightColor,
                                valueFontSize = 24
                            )
                        }
                    }

                    GlanceDivider()

                    // 3s smoothed
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("3s AVG", fontSize = 12)
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 18)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText(displayText, GlanceColors.Label, 18)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balance3sLeft.toInt()}", GlanceColors.White, 18)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText("${metrics.balance3s.toInt()}", GlanceColors.White, 18)
                            }
                        }
                    }

                    GlanceDivider()

                    // 10s smoothed
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("10s AVG", fontSize = 12)
                        if (noData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText(displayText, GlanceColors.Label, 18)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText(displayText, GlanceColors.Label, 18)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balance10sLeft.toInt()}", GlanceColors.White, 18)
                                ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 6.dp))
                                ValueText("${metrics.balance10s.toInt()}", GlanceColors.White, 18)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Current + 3s + 10s smoothed
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Current balance
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

                    GlanceDivider()

                    // 3s and 10s smoothed side by side
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // 3s section
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("3s AVG", fontSize = 12)
                            if (noData) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText(displayText, GlanceColors.Label, 16)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 2.dp))
                                    ValueText(displayText, GlanceColors.Label, 16)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText("${metrics.balance3sLeft.toInt()}", GlanceColors.White, 16)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 2.dp))
                                    ValueText("${metrics.balance3s.toInt()}", GlanceColors.White, 16)
                                }
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // 10s section
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("10s AVG", fontSize = 12)
                            if (noData) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText(displayText, GlanceColors.Label, 16)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 2.dp))
                                    ValueText(displayText, GlanceColors.Label, 16)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText("${metrics.balance10sLeft.toInt()}", GlanceColors.White, 16)
                                    ValueText("|", GlanceColors.Separator, 12, GlanceModifier.padding(horizontal = 2.dp))
                                    ValueText("${metrics.balance10s.toInt()}", GlanceColors.White, 16)
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
 * Get trend indicator based on current vs 3s smoothed balance.
 */
private fun getTrendIndicator(metrics: PedalingMetrics): Pair<String, Color> {
    val currentRight = metrics.balance
    val smoothedRight = metrics.balance3s
    val diff = currentRight - smoothedRight

    return when {
        diff > 1.5f -> "↗" to GlanceColors.Attention  // Moving right
        diff < -1.5f -> "↙" to GlanceColors.Attention // Moving left
        else -> "→" to GlanceColors.Optimal           // Stable
    }
}

/**
 * Balance Trend DataType using Glance.
 */
class BalanceTrendGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "balance-trend") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        BalanceTrendContent(metrics, config, sensorDisconnected)
    }
}
