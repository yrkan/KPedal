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
import io.github.kpedal.datatypes.BaseDataType
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Full Overview DataType content using Glance composables.
 * Shows Balance + TE + PS in one view.
 *
 * Layout sizes:
 * - SMALL: Very compact horizontal (BAL | TE avg | PS avg)
 * - SMALL_WIDE/MEDIUM_WIDE: Horizontal with balance L|R and TE/PS averages
 * - MEDIUM/NARROW: 3 rows (Balance, TE L/R, PS L/R)
 * - LARGE: Full layout with progress bars (simplified to no progress bars in Glance)
 */
@Composable
fun FullOverviewContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                FullOverviewSmall(metrics, sensorDisconnected)
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                FullOverviewSmallWide(metrics, sensorDisconnected)
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                FullOverviewMediumWide(metrics, sensorDisconnected)
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                FullOverviewMedium(metrics, sensorDisconnected)
            }
            BaseDataType.LayoutSize.NARROW -> {
                FullOverviewNarrow(metrics, sensorDisconnected)
            }
            BaseDataType.LayoutSize.LARGE -> {
                FullOverviewLarge(metrics, sensorDisconnected)
            }
        }
    }
}

/**
 * SMALL layout: Very compact, horizontal
 * Shows averages only - no L/R split (not enough space)
 * BAL avg | TE avg | PS avg
 */
@Composable
private fun FullOverviewSmall(
    metrics: PedalingMetrics,
    sensorDisconnected: Boolean
) {
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Balance section - show dominant side value only
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (noData) {
                ValueText(displayText, GlanceColors.Label, 18)
            } else {
                // Show the higher (dominant) side with color indicating imbalance
                val balanceStatus = StatusCalculator.balanceStatus(metrics.balance)
                val balanceColor = if (balanceStatus == StatusCalculator.Status.OPTIMAL) {
                    GlanceColors.White
                } else {
                    getStatusColor(balanceStatus)
                }
                // Show L:R ratio compactly
                val left = metrics.balanceLeft.toInt()
                val right = metrics.balance.toInt()
                ValueText("$left:$right", balanceColor, 14)
            }
            LabelText("BAL")
        }

        // TE section
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (noData) {
                ValueText(displayText, GlanceColors.Label, 18)
            } else {
                val teAvg = metrics.torqueEffAvg.toInt()
                val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                ValueText("$teAvg", teColor, 18)
            }
            LabelText("TE")
        }

        // PS section
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (noData) {
                ValueText(displayText, GlanceColors.Label, 18)
            } else {
                val psAvg = metrics.pedalSmoothAvg.toInt()
                val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                ValueText("$psAvg", psColor, 18)
            }
            LabelText("PS")
        }
    }
}

/**
 * SMALL_WIDE/MEDIUM_WIDE layout: Horizontal with balance L|R and TE/PS averages
 * [48|52] | TE 76 | PS 22
 */
@Composable
private fun FullOverviewSmallWide(
    metrics: PedalingMetrics,
    sensorDisconnected: Boolean
) {
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Balance section (larger)
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabelText("BAL")
            if (noData) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ValueText(displayText, GlanceColors.Label, 18)
                    ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                    ValueText(displayText, GlanceColors.Label, 18)
                }
            } else {
                val (leftColor, rightColor) = getBalanceColors(metrics)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 18)
                    ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                    ValueText("${metrics.balance.toInt()}", rightColor, 18)
                }
            }
        }

        // TE section
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabelText("TE")
            if (noData) {
                ValueText(displayText, GlanceColors.Label, 16)
            } else {
                val teAvg = metrics.torqueEffAvg.toInt()
                val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                ValueText("$teAvg", teColor, 16)
            }
        }

        // PS section
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabelText("PS")
            if (noData) {
                ValueText(displayText, GlanceColors.Label, 16)
            } else {
                val psAvg = metrics.pedalSmoothAvg.toInt()
                val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                ValueText("$psAvg", psColor, 16)
            }
        }
    }
}

/**
 * MEDIUM_WIDE layout: Horizontal with larger values
 * [48|52] | TE 76 | PS 22
 */
@Composable
private fun FullOverviewMediumWide(
    metrics: PedalingMetrics,
    sensorDisconnected: Boolean
) {
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Balance section (larger)
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabelText("BAL", fontSize = 11)
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

        // TE section
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabelText("TE", fontSize = 11)
            if (noData) {
                ValueText(displayText, GlanceColors.Label, 18)
            } else {
                val teAvg = metrics.torqueEffAvg.toInt()
                val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                ValueText("$teAvg", teColor, 18)
            }
        }

        // PS section
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabelText("PS", fontSize = 11)
            if (noData) {
                ValueText(displayText, GlanceColors.Label, 18)
            } else {
                val psAvg = metrics.pedalSmoothAvg.toInt()
                val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                ValueText("$psAvg", psColor, 18)
            }
        }
    }
}

/**
 * MEDIUM layout: 3 rows
 * BAL: 48 | 52
 * TE:  74 | 77
 * PS:  22 | 24
 */
@Composable
private fun FullOverviewMedium(
    metrics: PedalingMetrics,
    sensorDisconnected: Boolean
) {
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Balance row
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            if (noData) {
                MetricRow("BAL", displayText, displayText, GlanceColors.White, GlanceColors.White, valueFontSize = 18, labelFontSize = 11)
            } else {
                val (leftColor, rightColor) = getBalanceColors(metrics)
                MetricRow(
                    "BAL",
                    "${metrics.balanceLeft.toInt()}",
                    "${metrics.balance.toInt()}",
                    leftColor, rightColor,
                    valueFontSize = 18,
                    labelFontSize = 11
                )
            }
        }

        GlanceDivider()

        // TE row
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            if (noData) {
                MetricRow("TE", displayText, displayText, GlanceColors.White, GlanceColors.White, valueFontSize = 16, labelFontSize = 11)
            } else {
                val teLColor = getTEColor(metrics.torqueEffLeft)
                val teRColor = getTEColor(metrics.torqueEffRight)
                MetricRow(
                    "TE",
                    "${metrics.torqueEffLeft.toInt()}",
                    "${metrics.torqueEffRight.toInt()}",
                    teLColor, teRColor,
                    valueFontSize = 16,
                    labelFontSize = 11
                )
            }
        }

        GlanceDivider()

        // PS row
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            if (noData) {
                MetricRow("PS", displayText, displayText, GlanceColors.White, GlanceColors.White, valueFontSize = 16, labelFontSize = 11)
            } else {
                val psLColor = getPSColor(metrics.pedalSmoothLeft)
                val psRColor = getPSColor(metrics.pedalSmoothRight)
                MetricRow(
                    "PS",
                    "${metrics.pedalSmoothLeft.toInt()}",
                    "${metrics.pedalSmoothRight.toInt()}",
                    psLColor, psRColor,
                    valueFontSize = 16,
                    labelFontSize = 11
                )
            }
        }
    }
}

/**
 * NARROW layout: More vertical space - larger values
 * BAL: 48 | 52
 * TE:  74 | 77
 * PS:  22 | 24
 */
@Composable
private fun FullOverviewNarrow(
    metrics: PedalingMetrics,
    sensorDisconnected: Boolean
) {
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Balance row
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            if (noData) {
                MetricRow("BAL", displayText, displayText, GlanceColors.White, GlanceColors.White, valueFontSize = 20, labelFontSize = 12)
            } else {
                val (leftColor, rightColor) = getBalanceColors(metrics)
                MetricRow(
                    "BAL",
                    "${metrics.balanceLeft.toInt()}",
                    "${metrics.balance.toInt()}",
                    leftColor, rightColor,
                    valueFontSize = 20,
                    labelFontSize = 12
                )
            }
        }

        GlanceDivider()

        // TE row
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            if (noData) {
                MetricRow("TE", displayText, displayText, GlanceColors.White, GlanceColors.White, valueFontSize = 18, labelFontSize = 12)
            } else {
                val teLColor = getTEColor(metrics.torqueEffLeft)
                val teRColor = getTEColor(metrics.torqueEffRight)
                MetricRow(
                    "TE",
                    "${metrics.torqueEffLeft.toInt()}",
                    "${metrics.torqueEffRight.toInt()}",
                    teLColor, teRColor,
                    valueFontSize = 18,
                    labelFontSize = 12
                )
            }
        }

        GlanceDivider()

        // PS row
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            if (noData) {
                MetricRow("PS", displayText, displayText, GlanceColors.White, GlanceColors.White, valueFontSize = 18, labelFontSize = 12)
            } else {
                val psLColor = getPSColor(metrics.pedalSmoothLeft)
                val psRColor = getPSColor(metrics.pedalSmoothRight)
                MetricRow(
                    "PS",
                    "${metrics.pedalSmoothLeft.toInt()}",
                    "${metrics.pedalSmoothRight.toInt()}",
                    psLColor, psRColor,
                    valueFontSize = 18,
                    labelFontSize = 12
                )
            }
        }
    }
}

/**
 * LARGE layout: Full view with all details
 * Balance: L | R + avg bar (simplified - no progress bar in Glance)
 * TE: L | R + avg
 * PS: L | R + avg
 */
@Composable
private fun FullOverviewLarge(
    metrics: PedalingMetrics,
    sensorDisconnected: Boolean
) {
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Balance section
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LabelText("BALANCE", fontSize = 12)
            if (noData) {
                BalanceRow(displayText, displayText, GlanceColors.Label, GlanceColors.Label,
                    GlanceModifier.fillMaxSize(), valueFontSize = 22)
            } else {
                val (leftColor, rightColor) = getBalanceColors(metrics)
                BalanceRow(
                    "${metrics.balanceLeft.toInt()}",
                    "${metrics.balance.toInt()}",
                    leftColor, rightColor,
                    GlanceModifier.fillMaxSize(),
                    valueFontSize = 22
                )
            }
        }

        GlanceDivider()

        // TE section
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TE label and avg
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                LabelText("TE", fontSize = 12)
                if (noData) {
                    ValueText(displayText, GlanceColors.Label, 12)
                } else {
                    val teAvg = metrics.torqueEffAvg.toInt()
                    val teAvgColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                    ValueText("$teAvg", teAvgColor, 12)
                }
            }

            // TE L/R values
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (noData) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        }
                        GlanceVerticalDivider(height = 20)
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        }
                    }
                } else {
                    val teLColor = getTEColor(metrics.torqueEffLeft)
                    val teRColor = getTEColor(metrics.torqueEffRight)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText("${metrics.torqueEffLeft.toInt()}", teLColor, 18)
                        }
                        GlanceVerticalDivider(height = 20)
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText("${metrics.torqueEffRight.toInt()}", teRColor, 18)
                        }
                    }
                }
            }
        }

        GlanceDivider()

        // PS section
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PS label and avg
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                LabelText("PS", fontSize = 12)
                if (noData) {
                    ValueText(displayText, GlanceColors.Label, 12)
                } else {
                    val psAvg = metrics.pedalSmoothAvg.toInt()
                    val psAvgColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                    ValueText("$psAvg", psAvgColor, 12)
                }
            }

            // PS L/R values
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (noData) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        }
                        GlanceVerticalDivider(height = 20)
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        }
                    }
                } else {
                    val psLColor = getPSColor(metrics.pedalSmoothLeft)
                    val psRColor = getPSColor(metrics.pedalSmoothRight)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText("${metrics.pedalSmoothLeft.toInt()}", psLColor, 18)
                        }
                        GlanceVerticalDivider(height = 20)
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            ValueText("${metrics.pedalSmoothRight.toInt()}", psRColor, 18)
                        }
                    }
                }
            }
        }
    }
}
