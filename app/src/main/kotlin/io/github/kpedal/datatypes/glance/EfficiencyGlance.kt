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
import io.github.kpedal.KPedalExtension
import io.github.kpedal.datatypes.BaseDataType
import io.github.kpedal.datatypes.GlanceDataType
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.hammerhead.karooext.models.ViewConfig

/**
 * Efficiency DataType content using Glance.
 * Shows TE (Torque Effectiveness) + PS (Pedal Smoothness).
 */
@Composable
fun EfficiencyContent(
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
                // TE + PS horizontal with larger fonts
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TE
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
                    // PS
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
            BaseDataType.LayoutSize.SMALL_WIDE -> {
                // TE + PS horizontal
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TE section
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LabelText("TE")
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 20)
                            } else {
                                val teAvg = metrics.torqueEffAvg.toInt()
                                val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                                ValueText("$teAvg", teColor, 20)
                            }
                        }
                    }
                    // PS section
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LabelText("PS")
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 20)
                            } else {
                                val psAvg = metrics.pedalSmoothAvg.toInt()
                                val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                                ValueText("$psAvg", psColor, 20)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM_WIDE -> {
                // TE + PS horizontal with larger values
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TE section
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LabelText("TE", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                val teAvg = metrics.torqueEffAvg.toInt()
                                val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                                ValueText("$teAvg", teColor, 22)
                            }
                        }
                    }
                    // PS section
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LabelText("PS", fontSize = 11)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 22)
                            } else {
                                val psAvg = metrics.pedalSmoothAvg.toInt()
                                val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                                ValueText("$psAvg", psColor, 22)
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.MEDIUM -> {
                // TE + PS with L/R values
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // TE section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                LabelText("TE", fontSize = 11)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 12)
                                } else {
                                    val teAvg = metrics.torqueEffAvg.toInt()
                                    val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                                    ValueText("$teAvg", teColor, 12)
                                }
                            }
                            Box(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (noData) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText(displayText, GlanceColors.Label, 18)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 20)
                                        ValueText(displayText, GlanceColors.Label, 18)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText("${metrics.torqueEffLeft.toInt()}", getTEColor(metrics.torqueEffLeft), 18)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 20)
                                        ValueText("${metrics.torqueEffRight.toInt()}", getTEColor(metrics.torqueEffRight), 18)
                                    }
                                }
                            }
                        }
                    }

                    GlanceDivider()

                    // PS section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                LabelText("PS", fontSize = 11)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 12)
                                } else {
                                    val psAvg = metrics.pedalSmoothAvg.toInt()
                                    val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                                    ValueText("$psAvg", psColor, 12)
                                }
                            }
                            Box(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (noData) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText(displayText, GlanceColors.Label, 18)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 20)
                                        ValueText(displayText, GlanceColors.Label, 18)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText("${metrics.pedalSmoothLeft.toInt()}", getPSColor(metrics.pedalSmoothLeft), 18)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 20)
                                        ValueText("${metrics.pedalSmoothRight.toInt()}", getPSColor(metrics.pedalSmoothRight), 18)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.NARROW -> {
                // NARROW: more vertical space - larger values with L/R labels
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // TE section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                LabelText("TE", fontSize = 12)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 14)
                                } else {
                                    val teAvg = metrics.torqueEffAvg.toInt()
                                    val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                                    ValueText("$teAvg", teColor, 14)
                                }
                            }
                            Box(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (noData) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText(displayText, GlanceColors.Label, 22)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 24)
                                        ValueText(displayText, GlanceColors.Label, 22)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText("${metrics.torqueEffLeft.toInt()}", getTEColor(metrics.torqueEffLeft), 22)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 24)
                                        ValueText("${metrics.torqueEffRight.toInt()}", getTEColor(metrics.torqueEffRight), 22)
                                    }
                                }
                            }
                        }
                    }

                    GlanceDivider()

                    // PS section
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                LabelText("PS", fontSize = 12)
                                if (noData) {
                                    ValueText(displayText, GlanceColors.Label, 14)
                                } else {
                                    val psAvg = metrics.pedalSmoothAvg.toInt()
                                    val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                                    ValueText("$psAvg", psColor, 14)
                                }
                            }
                            Box(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (noData) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText(displayText, GlanceColors.Label, 22)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 24)
                                        ValueText(displayText, GlanceColors.Label, 22)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ValueText("${metrics.pedalSmoothLeft.toInt()}", getPSColor(metrics.pedalSmoothLeft), 22)
                                        GlanceVerticalDivider(GlanceModifier.padding(horizontal = 8.dp), height = 24)
                                        ValueText("${metrics.pedalSmoothRight.toInt()}", getPSColor(metrics.pedalSmoothRight), 22)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            BaseDataType.LayoutSize.LARGE -> {
                // Full layout with L/R labels
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // TE section
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            LabelText("TE", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 12)
                            } else {
                                val teAvg = metrics.torqueEffAvg.toInt()
                                val teColor = getStatusColor(StatusCalculator.teStatus(metrics.torqueEffAvg))
                                ValueText("$teAvg", teColor, 12)
                            }
                        }
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                        ValueText(displayText, GlanceColors.Label, 20)
                                    } else {
                                        ValueText("${metrics.torqueEffLeft.toInt()}", getTEColor(metrics.torqueEffLeft), 20)
                                    }
                                }
                                Column(
                                    modifier = GlanceModifier.defaultWeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LabelText("R", fontSize = 12)
                                    if (noData) {
                                        ValueText(displayText, GlanceColors.Label, 20)
                                    } else {
                                        ValueText("${metrics.torqueEffRight.toInt()}", getTEColor(metrics.torqueEffRight), 20)
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
                        Column(horizontalAlignment = Alignment.Start) {
                            LabelText("PS", fontSize = 12)
                            if (noData) {
                                ValueText(displayText, GlanceColors.Label, 12)
                            } else {
                                val psAvg = metrics.pedalSmoothAvg.toInt()
                                val psColor = getStatusColor(StatusCalculator.psStatus(metrics.pedalSmoothAvg))
                                ValueText("$psAvg", psColor, 12)
                            }
                        }
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                        ValueText(displayText, GlanceColors.Label, 20)
                                    } else {
                                        ValueText("${metrics.pedalSmoothLeft.toInt()}", getPSColor(metrics.pedalSmoothLeft), 20)
                                    }
                                }
                                Column(
                                    modifier = GlanceModifier.defaultWeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LabelText("R", fontSize = 12)
                                    if (noData) {
                                        ValueText(displayText, GlanceColors.Label, 20)
                                    } else {
                                        ValueText("${metrics.pedalSmoothRight.toInt()}", getPSColor(metrics.pedalSmoothRight), 20)
                                    }
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
 * Efficiency DataType using Glance.
 */
class EfficiencyGlanceDataType(
    kpedalExtension: KPedalExtension
) : GlanceDataType(kpedalExtension, "efficiency") {

    @Composable
    override fun Content(
        metrics: PedalingMetrics,
        config: ViewConfig,
        sensorDisconnected: Boolean
    ) {
        EfficiencyContent(metrics, config, sensorDisconnected)
    }
}
