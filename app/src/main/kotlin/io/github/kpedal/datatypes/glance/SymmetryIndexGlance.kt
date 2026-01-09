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
import kotlin.math.abs

/**
 * Symmetry Index DataType content using Glance.
 * Shows pedal stroke symmetry as a percentage.
 * 100% = perfect balance, lower = more asymmetric.
 *
 * Formula: 100 - |50 - balance| * 2
 * Examples:
 * - 50:50 → 100%
 * - 48:52 → 96%
 * - 45:55 → 90%
 * - 40:60 → 80%
 */
@Composable
fun SymmetryIndexContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    // Calculate symmetry indices
    val symmetry = calculateSymmetry(metrics.balance)
    val symmetry3s = calculateSymmetry(metrics.balance3s)
    val symmetry10s = calculateSymmetry(metrics.balance10s)
    val symmetryColor = getSymmetryColor(symmetry)

    // Calculate trend: current vs 10s average
    val trend = getSymmetryTrend(symmetry, symmetry10s)

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Symmetry % with trend arrow
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (noData) {
                        ValueText(displayText, GlanceColors.Label, 18)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ValueText("$symmetry%", symmetryColor, 20)
                            if (trend != 0) {
                                val trendArrow = if (trend > 0) "▲" else "▼"
                                val trendColor = if (trend > 0) GlanceColors.Optimal else GlanceColors.Attention
                                ValueText(trendArrow, trendColor, 12, GlanceModifier.padding(start = 2.dp))
                            }
                        }
                    }
                    LabelText("SYM")
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Symmetry + Balance
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Symmetry with trend
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("$symmetry%", symmetryColor, 18)
                                if (trend != 0) {
                                    val trendArrow = if (trend > 0) "▲" else "▼"
                                    val trendColor = if (trend > 0) GlanceColors.Optimal else GlanceColors.Attention
                                    ValueText(trendArrow, trendColor, 10, GlanceModifier.padding(start = 2.dp))
                                }
                            }
                        }
                        LabelText("SYM")
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
                        LabelText("L:R")
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Symmetry + Trend + Balance
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Symmetry
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("SYM", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            ValueText("$symmetry%", symmetryColor, 22)
                        }
                    }
                    // Trend indicator
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("TREND", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            val (trendText, trendColor) = getTrendDisplay(trend)
                            ValueText(trendText, trendColor, 18)
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
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Symmetry with trend (top) + Balance (bottom)
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Symmetry section with trend
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("SYMMETRY", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 24)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("$symmetry%", symmetryColor, 26)
                                if (trend != 0) {
                                    val trendArrow = if (trend > 0) "▲" else "▼"
                                    val trendColor = if (trend > 0) GlanceColors.Optimal else GlanceColors.Attention
                                    ValueText(trendArrow, trendColor, 14, GlanceModifier.padding(start = 4.dp))
                                }
                            }
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
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // Symmetry + Trend history + Balance
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Symmetry section with quality
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("SYMMETRY INDEX", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 28)
                        } else {
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("$symmetry", symmetryColor, 32)
                                ValueText("%", symmetryColor, 18, GlanceModifier.padding(start = 2.dp, bottom = 4.dp))
                            }
                            val qualityText = getSymmetryQuality(symmetry)
                            LabelText(qualityText, GlanceModifier.padding(top = 2.dp), fontSize = 10)
                        }
                    }

                    GlanceDivider()

                    // Trend: NOW vs 3s vs 10s
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("NOW", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("$symmetry%", symmetryColor, 18)
                            }
                        }

                        GlanceVerticalDivider()

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("3s", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("$symmetry3s%", getSymmetryColor(symmetry3s), 18)
                            }
                        }

                        GlanceVerticalDivider()

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("10s", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 16)
                            } else {
                                ValueText("$symmetry10s%", getSymmetryColor(symmetry10s), 18)
                            }
                        }
                    }

                    GlanceDivider()

                    // Balance with L/R labels
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
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full symmetry dashboard with trend history
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header with quality
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("SYMMETRY INDEX", fontSize = 12)
                        if (!noData) {
                            val qualityText = getSymmetryQuality(symmetry)
                            ValueText(" · $qualityText", symmetryColor, 12, GlanceModifier.padding(start = 4.dp))
                        }
                    }

                    GlanceDivider()

                    // Symmetry main display with trend
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 28)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("$symmetry", symmetryColor, 34)
                                ValueText("%", symmetryColor, 20, GlanceModifier.padding(start = 2.dp, bottom = 4.dp))
                                if (trend != 0) {
                                    val trendArrow = if (trend > 0) "▲" else "▼"
                                    val trendColor = if (trend > 0) GlanceColors.Optimal else GlanceColors.Attention
                                    ValueText(trendArrow, trendColor, 18, GlanceModifier.padding(start = 6.dp))
                                }
                            }
                        }
                    }

                    GlanceDivider()

                    // Trend history: 3s and 10s
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // 3s average
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("3s AVG", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                ValueText("$symmetry3s%", getSymmetryColor(symmetry3s), 20)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // 10s average
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("10s AVG", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                ValueText("$symmetry10s%", getSymmetryColor(symmetry10s), 20)
                            }
                        }
                    }

                    GlanceDivider()

                    // L/R breakdown
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Left
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("LEFT", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val (leftColor, _) = getBalanceColors(metrics)
                                ValueText("${metrics.balanceLeft.toInt()}%", leftColor, 18)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // Right
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("RIGHT", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                val (_, rightColor) = getBalanceColors(metrics)
                                ValueText("${metrics.balance.toInt()}%", rightColor, 18)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate symmetry index from balance.
 * 100% = perfect symmetry, lower = more asymmetric.
 */
private fun calculateSymmetry(balance: Float): Int {
    val deviation = abs(50 - balance)
    return (100 - deviation * 2).toInt().coerceIn(0, 100)
}

/**
 * Get color for symmetry value.
 */
private fun getSymmetryColor(symmetry: Int): Color {
    return when {
        symmetry >= 96 -> GlanceColors.Optimal
        symmetry >= 90 -> GlanceColors.White
        symmetry >= 85 -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
}

/**
 * Get quality text for symmetry value.
 */
private fun getSymmetryQuality(symmetry: Int): String {
    return when {
        symmetry >= 96 -> "EXCELLENT"
        symmetry >= 90 -> "GOOD"
        symmetry >= 85 -> "FAIR"
        else -> "IMBALANCED"
    }
}

/**
 * Calculate symmetry trend: current vs 10s average.
 * Returns: 1 = improving, 0 = stable, -1 = degrading
 */
private fun getSymmetryTrend(current: Int, avg10s: Int): Int {
    val diff = current - avg10s
    return when {
        diff >= 2 -> 1   // Improving (at least 2% better)
        diff <= -2 -> -1 // Degrading (at least 2% worse)
        else -> 0        // Stable
    }
}

/**
 * Get trend display text and color.
 */
private fun getTrendDisplay(trend: Int): Pair<String, Color> {
    return when (trend) {
        1 -> "▲" to GlanceColors.Optimal
        -1 -> "▼" to GlanceColors.Attention
        else -> "●" to GlanceColors.White
    }
}

/**
 * Symmetry Index DataType using Glance.
 */
class SymmetryIndexGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "symmetry-index") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        SymmetryIndexContent(metrics, config, sensorDisconnected)
    }
}
