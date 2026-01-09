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
 * HR + Efficiency DataType content using Glance.
 * Shows heart rate alongside pedaling efficiency (TE/PS).
 * Helps identify when technique drops as HR increases (fatigue).
 */
@Composable
fun HREfficiencyContent(
    metrics: PedalingMetrics,
    config: ViewConfig,
    sensorDisconnected: Boolean
) {
    val layoutSize = BaseDataType.getLayoutSize(config)
    val noData = sensorDisconnected || !metrics.hasData
    val displayText = if (sensorDisconnected) BaseDataType.SENSOR_DISCONNECTED else BaseDataType.NO_DATA

    val hr = metrics.heartRate
    val teAvg = ((metrics.torqueEffLeft + metrics.torqueEffRight) / 2).toInt()
    val psAvg = ((metrics.pedalSmoothLeft + metrics.pedalSmoothRight) / 2).toInt()
    val hasHR = hr > 0

    DataFieldContainer {
        when (layoutSize) {
            BaseDataType.LayoutSize.SMALL -> {
                // HR only with zone color
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!hasHR) {
                        ValueText("--", GlanceColors.Label, 20)
                    } else {
                        val hrColor = getHRColor(hr)
                        ValueText("$hr", hrColor, 24)
                    }
                    LabelText("HR")
                }
            }
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // HR + TE
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // HR
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!hasHR) {
                            ValueText("--", GlanceColors.Label, 18)
                        } else {
                            val hrColor = getHRColor(hr)
                            ValueText("$hr", hrColor, 20)
                        }
                        LabelText("HR")
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
                            ValueText("$teAvg", getTEColor(teAvg.toFloat()), 18)
                        }
                        LabelText("TE")
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // HR + TE + PS
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // HR
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("HR", fontSize = 11)
                        if (!hasHR) {
                            ValueText("--", GlanceColors.Label, 20)
                        } else {
                            val hrColor = getHRColor(hr)
                            ValueText("$hr", hrColor, 22)
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
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            ValueText("$teAvg", getTEColor(teAvg.toFloat()), 20)
                        }
                    }
                    // PS
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PS", fontSize = 11)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 18)
                        } else {
                            ValueText("$psAvg", getPSColor(psAvg.toFloat()), 20)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // HR (top) + TE/PS (bottom)
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // HR section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("HEART RATE", fontSize = 11)
                        if (!hasHR) {
                            ValueText("--", GlanceColors.Label, 26)
                        } else {
                            val hrColor = getHRColor(hr)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("$hr", hrColor, 28)
                                LabelText("bpm", GlanceModifier.padding(start = 2.dp, bottom = 4.dp), fontSize = 10)
                            }
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
                                ValueText("$teAvg", getTEColor(teAvg.toFloat()), 18)
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
                                ValueText("$psAvg", getPSColor(psAvg.toFloat()), 18)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // HR + TE + PS stacked with efficiency indicator
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // HR section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("HEART RATE", fontSize = 12)
                        if (!hasHR) {
                            ValueText("--", GlanceColors.Label, 28)
                        } else {
                            val hrColor = getHRColor(hr)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("$hr", hrColor, 32)
                                LabelText("bpm", GlanceModifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 11)
                            }
                        }
                    }

                    GlanceDivider()

                    // TE section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("TORQUE EFF", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            ValueText("$teAvg", getTEColor(teAvg.toFloat()), 22)
                        }
                    }

                    GlanceDivider()

                    // PS section
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("PEDAL SMOOTH", fontSize = 12)
                        if (noData) {
                            ValueText(displayText, GlanceColors.Label, 20)
                        } else {
                            ValueText("$psAvg", getPSColor(psAvg.toFloat()), 22)
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full HR + Efficiency dashboard
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header with efficiency indicator
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("HR + EFFICIENCY", fontSize = 12)
                        if (!noData && hasHR) {
                            val efficiencyStatus = getEfficiencyStatus(teAvg, psAvg)
                            ValueText(" $efficiencyStatus", getEfficiencyColor(teAvg, psAvg), 12, GlanceModifier.padding(start = 4.dp))
                        }
                    }

                    GlanceDivider()

                    // HR section - larger
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelText("HEART RATE", fontSize = 12)
                        if (!hasHR) {
                            ValueText("--", GlanceColors.Label, 28)
                        } else {
                            val hrColor = getHRColor(hr)
                            Row(verticalAlignment = Alignment.Bottom) {
                                ValueText("$hr", hrColor, 30)
                                LabelText("bpm", GlanceModifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 12)
                            }
                        }
                    }

                    GlanceDivider()

                    // TE and PS row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        // TE
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("TORQUE EFF", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                ValueText("$teAvg", getTEColor(teAvg.toFloat()), 20)
                            }
                        }

                        GlanceVerticalDivider(GlanceModifier.padding(vertical = 8.dp))

                        // PS
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelText("PEDAL SMOOTH", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 18)
                            } else {
                                ValueText("$psAvg", getPSColor(psAvg.toFloat()), 20)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get HR color based on approximate zones.
 * Without FTP/max HR, use generic ranges.
 */
private fun getHRColor(hr: Int): Color {
    return when {
        hr < 100 -> GlanceColors.White      // Recovery
        hr < 130 -> GlanceColors.Optimal    // Endurance
        hr < 150 -> GlanceColors.White      // Tempo
        hr < 170 -> GlanceColors.Attention  // Threshold
        else -> GlanceColors.Problem        // VO2max+
    }
}

/**
 * Get efficiency status text.
 */
private fun getEfficiencyStatus(te: Int, ps: Int): String {
    val score = (te + ps) / 2
    return when {
        score >= 50 -> "●"
        score >= 35 -> "●"
        else -> "●"
    }
}

/**
 * Get efficiency color based on TE + PS average.
 */
private fun getEfficiencyColor(te: Int, ps: Int): Color {
    val teOk = te >= 60
    val psOk = ps >= 18
    return when {
        teOk && psOk -> GlanceColors.Optimal
        teOk || psOk -> GlanceColors.White
        else -> GlanceColors.Attention
    }
}

/**
 * HR + Efficiency DataType using Glance.
 */
class HREfficiencyGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "hr-efficiency") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        HREfficiencyContent(metrics, config, sensorDisconnected)
    }
}
