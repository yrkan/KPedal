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
 * Sprint Mode DataType content using Glance.
 * Optimized for high-intensity efforts and intervals.
 * Large power display, cadence, and balance for quick glances.
 */
@Composable
fun SprintModeContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    val power = metrics.power
    val cadence = metrics.cadence
    val grade = metrics.grade

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Power only - HUGE font for quick glances
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ValueText("$power", GlanceColors.White, 28)
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Power + Cadence
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power - dominant
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 24)
                            LabelText("w", GlanceModifier.padding(start = 1.dp, bottom = 3.dp))
                        }
                    }
                    // Cadence
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$cadence", getCadenceSprintColor(cadence), 20)
                            LabelText("rpm", GlanceModifier.padding(start = 1.dp, bottom = 2.dp))
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Power + Cadence + Balance
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
                            ValueText("$power", GlanceColors.White, 24)
                            LabelText("w", GlanceModifier.padding(start = 1.dp, bottom = 3.dp), fontSize = 10)
                        }
                    }
                    // Cadence
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ValueText("$cadence", getCadenceSprintColor(cadence), 20)
                        LabelText("RPM", fontSize = 10)
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
                        LabelText("BAL", fontSize = 10)
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Power (huge) + Cadence/Balance row
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Power - maximum emphasis
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 36)
                            LabelText("W", GlanceModifier.padding(start = 2.dp, bottom = 6.dp), fontSize = 12)
                        }
                    }

                    GlanceDivider()

                    // Cadence and Balance
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Cadence
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ValueText("$cadence", getCadenceSprintColor(cadence), 18)
                            LabelText("RPM", fontSize = 11)
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
                            LabelText("BAL", fontSize = 11)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // Power + Cadence + Balance + Grade (for hill sprints)
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Power - main focus
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("POWER", fontSize = 12)
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 36)
                            LabelText("W", GlanceModifier.padding(start = 4.dp, bottom = 6.dp), fontSize = 14)
                        }
                    }

                    GlanceDivider()

                    // Cadence + Grade
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Cadence
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("CAD", fontSize = 12)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("$cadence", getCadenceSprintColor(cadence), 20)
                            }
                        }

                        GlanceVerticalDivider()

                        // Grade
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("GRADE", fontSize = 12)
                            val gradeDisplay = if (grade >= 0) "+${grade.toInt()}%" else "${grade.toInt()}%"
                            ValueText(gradeDisplay, getGradeSprintColor(grade), 18)
                        }
                    }

                    GlanceDivider()

                    // Balance
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
                // Sprint dashboard - power dominant
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header with sprint indicator
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isHighPower = power > 300
                        val indicatorColor = if (isHighPower) GlanceColors.Problem else GlanceColors.Label
                        ValueText("●", indicatorColor, 12)
                        LabelText(" SPRINT MODE", GlanceModifier.padding(start = 4.dp), fontSize = 12)
                    }

                    GlanceDivider()

                    // Power - maximum size
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("$power", GlanceColors.White, 40)
                            LabelText("W", GlanceModifier.padding(start = 4.dp, bottom = 8.dp), fontSize = 16)
                        }
                    }

                    GlanceDivider()

                    // Cadence + Balance + Grade row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // Cadence
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("CAD", fontSize = 12)
                            ValueText("$cadence", getCadenceSprintColor(cadence), 18)
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

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
                                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 14)
                                    ValueText(":", GlanceColors.Separator, 10)
                                    ValueText("${metrics.balance.toInt()}", rightColor, 14)
                                }
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // Grade
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("GRADE", fontSize = 12)
                            val gradeDisplay = if (grade >= 0) "+${grade.toInt()}%" else "${grade.toInt()}%"
                            ValueText(gradeDisplay, getGradeSprintColor(grade), 16)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get cadence color for sprint mode.
 * Higher cadence = better for sprints.
 */
private fun getCadenceSprintColor(cadence: Int): Color {
    return when {
        cadence >= 100 -> GlanceColors.Optimal   // High spin
        cadence >= 85 -> GlanceColors.White      // Good
        cadence >= 70 -> GlanceColors.Attention  // Low
        else -> GlanceColors.Problem             // Too low
    }
}

/**
 * Get grade color for sprint mode.
 */
private fun getGradeSprintColor(grade: Float): Color {
    return when {
        grade < -2 -> GlanceColors.Optimal       // Downhill advantage
        grade < 2 -> GlanceColors.White          // Flat
        grade < 5 -> GlanceColors.Attention      // Uphill
        else -> GlanceColors.Problem             // Hard climb
    }
}

/**
 * Sprint Mode DataType using Glance.
 */
class SprintModeGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "sprint-mode") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        SprintModeContent(metrics, config, sensorDisconnected)
    }
}
