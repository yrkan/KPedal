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
 * Climbing Mode DataType content using Glance.
 * Optimized for hill climbing - shows grade, power, and balance.
 * Activates when grade > 3%.
 */
@Composable
fun ClimbingModeContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    val grade = metrics.grade
    val isClimbing = grade > 3f
    val gradeDisplay = if (grade >= 0) "+${grade.toInt()}%" else "${grade.toInt()}%"
    val elevGain = metrics.elevationGain.toInt()

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // Grade + climbing indicator
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val gradeColor = getGradeColor(grade)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isClimbing) {
                            ValueText("▲", gradeColor, 18, GlanceModifier.padding(end = 2.dp))
                        }
                        ValueText(gradeDisplay, gradeColor, 24)
                    }
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // Grade + Power
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Grade
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val gradeColor = getGradeColor(grade)
                        ValueText(gradeDisplay, gradeColor, 22)
                    }
                    // Power
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            ValueText("${metrics.power}", GlanceColors.White, 22)
                            LabelText("w", GlanceModifier.padding(start = 1.dp, bottom = 3.dp))
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // Grade + Power + Balance
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Grade
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val gradeColor = getGradeColor(grade)
                        ValueText(gradeDisplay, gradeColor, 20)
                        LabelText("GRADE", fontSize = 11)
                    }
                    // Power
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ValueText("${metrics.power}", GlanceColors.White, 20)
                        LabelText("W", fontSize = 11)
                    }
                    // Balance
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 16)
                        } else {
                            val (leftColor, rightColor) = getBalanceColors(metrics)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValueText("${metrics.balanceLeft.toInt()}", leftColor, 16)
                                ValueText(":", GlanceColors.Separator, 12)
                                ValueText("${metrics.balance.toInt()}", rightColor, 16)
                            }
                        }
                        LabelText("BAL", fontSize = 11)
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // Grade + Power/Balance stacked
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Grade section - large
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("GRADE", fontSize = 11)
                        val gradeColor = getGradeColor(grade)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isClimbing) {
                                ValueText("▲", gradeColor, 20, GlanceModifier.padding(end = 4.dp))
                            }
                            ValueText(gradeDisplay, gradeColor, 26)
                        }
                    }

                    GlanceDivider()

                    // Power + Balance
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PWR", fontSize = 11)
                            ValueText("${metrics.power}", GlanceColors.White, 18)
                        }
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
                                    ValueText(":", GlanceColors.Separator, 12)
                                    ValueText("${metrics.balance.toInt()}", rightColor, 16)
                                }
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // Grade + Elev Gain + Power + Balance
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Grade section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("GRADE", fontSize = 12)
                        val gradeColor = getGradeColor(grade)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isClimbing) {
                                ValueText("▲", gradeColor, 22, GlanceModifier.padding(end = 4.dp))
                            }
                            ValueText(gradeDisplay, gradeColor, 28)
                        }
                    }

                    GlanceDivider()

                    // Elevation Gain + Power
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("ELEV", fontSize = 12)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("$elevGain", GlanceColors.White, 22)
                                LabelText("m", GlanceModifier.padding(start = 2.dp, bottom = 2.dp))
                            }
                        }

                        GlanceVerticalDivider()

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PWR", fontSize = 12)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("${metrics.power}", GlanceColors.White, 22)
                                LabelText("w", GlanceModifier.padding(start = 2.dp, bottom = 2.dp))
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
                // Full climbing dashboard
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Grade + Elevation
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("GRADE", fontSize = 12)
                            val gradeColor = getGradeColor(grade)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isClimbing) {
                                    ValueText("▲", gradeColor, 24, GlanceModifier.padding(end = 4.dp))
                                }
                                ValueText(gradeDisplay, gradeColor, 36)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("ELEV+", fontSize = 12)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("$elevGain", GlanceColors.White, 28)
                                LabelText("m", GlanceModifier.padding(start = 2.dp, bottom = 2.dp), fontSize = 12)
                            }
                        }
                    }

                    GlanceDivider()

                    // Power + Balance
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("POWER", fontSize = 12)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("${metrics.power}", GlanceColors.White, 28)
                                LabelText("W", GlanceModifier.padding(start = 2.dp, bottom = 2.dp), fontSize = 12)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("BALANCE", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 24)
                            } else {
                                val (leftColor, rightColor) = getBalanceColors(metrics)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ValueText("${metrics.balanceLeft.toInt()}", leftColor, 24)
                                    ValueText("|", GlanceColors.Separator, 14, GlanceModifier.padding(horizontal = 4.dp))
                                    ValueText("${metrics.balance.toInt()}", rightColor, 24)
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
 * Get color based on grade steepness.
 * Steeper = more attention required.
 */
private fun getGradeColor(grade: Float): Color {
    return when {
        grade < 0 -> GlanceColors.White           // Descending
        grade < 3 -> GlanceColors.White           // Flat
        grade < 6 -> GlanceColors.Optimal         // Easy climb
        grade < 10 -> GlanceColors.Attention      // Moderate climb
        else -> GlanceColors.Problem              // Hard climb
    }
}

/**
 * Climbing Mode DataType using Glance.
 */
class ClimbingModeGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "climbing-mode") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        ClimbingModeContent(metrics, config, sensorDisconnected)
    }
}
