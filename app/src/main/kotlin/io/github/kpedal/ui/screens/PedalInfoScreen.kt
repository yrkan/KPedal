package io.github.kpedal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.data.models.PedalInfo
import io.github.kpedal.data.models.SensorInfo
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.SensorStreamState
import io.github.kpedal.ui.theme.Theme
import java.util.Locale

/**
 * Screen showing pedal connection status, signal quality, and live data preview.
 */
@Composable
fun PedalInfoScreen(
    pedalInfo: PedalInfo,
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Theme.colors.dim,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() }
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.pedal_status),
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Divider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Connection Status Card
            ConnectionStatusCard(pedalInfo)

            Spacer(modifier = Modifier.height(16.dp))

            // Live Metrics Preview (only when connected)
            if (pedalInfo.isConnected && metrics.hasData) {
                LiveMetricsSection(metrics)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Signal Details
            SignalDetailsSection(pedalInfo)

            // Sensor Diagnostics (always show for debugging)
            Spacer(modifier = Modifier.height(16.dp))
            SensorDiagnosticsSection(pedalInfo)

            // Troubleshooting (when not connected or poor signal)
            if (!pedalInfo.isConnected || pedalInfo.signalQuality == PedalInfo.SignalQuality.POOR ||
                !pedalInfo.hasPowerMeter || !pedalInfo.hasCyclingDynamics) {
                Spacer(modifier = Modifier.height(16.dp))
                TroubleshootingSection(pedalInfo)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Theme.colors.divider)
    )
}

/**
 * Icon type for status indicator - drawn with Canvas for perfect centering.
 */
private enum class StatusIconType {
    CHECKMARK,      // ✓ - Connected/Streaming
    SEARCHING,      // Animated dots in circle
    CROSS,          // × - Disconnected/NotAvailable
    EMPTY_CIRCLE    // ○ - Idle
}

@Composable
private fun ConnectionStatusCard(pedalInfo: PedalInfo) {
    // Determine status based on sensorState (not just isConnected boolean)
    val (statusColor, iconType, statusText) = when (pedalInfo.sensorState) {
        is SensorStreamState.Streaming -> Triple(
            Theme.colors.optimal,
            StatusIconType.CHECKMARK,
            stringResource(R.string.connected)
        )
        is SensorStreamState.Searching -> Triple(
            Theme.colors.attention,
            StatusIconType.SEARCHING,
            stringResource(R.string.searching)
        )
        is SensorStreamState.NotAvailable -> Triple(
            Theme.colors.problem,
            StatusIconType.CROSS,
            stringResource(R.string.not_available)
        )
        is SensorStreamState.Disconnected -> Triple(
            Theme.colors.problem,
            StatusIconType.CROSS,
            stringResource(R.string.disconnected)
        )
        is SensorStreamState.Idle -> Triple(
            Theme.colors.dim,
            StatusIconType.EMPTY_CIRCLE,
            stringResource(R.string.idle)
        )
    }
    val bgColor = statusColor.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator with Canvas-drawn icon
            StatusIcon(
                iconType = iconType,
                backgroundColor = statusColor,
                iconColor = Theme.colors.background
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pedalInfo.modelName,
                    color = Theme.colors.dim,
                    fontSize = 12.sp
                )
            }

            // Signal bars (WiFi-style) - only show when streaming
            if (pedalInfo.sensorState is SensorStreamState.Streaming) {
                SignalBars(pedalInfo.signalQuality)
            }
        }
    }
}

/**
 * Status icon drawn with Canvas for perfect centering.
 */
@Composable
private fun StatusIcon(
    iconType: StatusIconType,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(48.dp)
    ) {
        val circleRadius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Draw background circle
        drawCircle(
            color = backgroundColor,
            radius = circleRadius,
            center = center
        )

        // Draw icon in center
        val iconSize = circleRadius * 0.5f
        val strokeWidth = 3.dp.toPx()

        when (iconType) {
            StatusIconType.CHECKMARK -> {
                // Draw checkmark ✓
                val path = androidx.compose.ui.graphics.Path().apply {
                    // Start from left-bottom of checkmark
                    moveTo(center.x - iconSize * 0.6f, center.y)
                    // Line to bottom of checkmark
                    lineTo(center.x - iconSize * 0.1f, center.y + iconSize * 0.5f)
                    // Line to top-right of checkmark
                    lineTo(center.x + iconSize * 0.7f, center.y - iconSize * 0.4f)
                }
                drawPath(
                    path = path,
                    color = iconColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }

            StatusIconType.SEARCHING -> {
                // Draw dotted circle (4 dots arranged in a circle)
                val dotRadius = 3.dp.toPx()
                val dotDistance = iconSize * 0.8f
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90 - 45).toDouble())
                    val dotCenter = Offset(
                        center.x + (dotDistance * kotlin.math.cos(angle)).toFloat(),
                        center.y + (dotDistance * kotlin.math.sin(angle)).toFloat()
                    )
                    drawCircle(
                        color = iconColor,
                        radius = dotRadius,
                        center = dotCenter
                    )
                }
            }

            StatusIconType.CROSS -> {
                // Draw X
                val crossSize = iconSize * 0.6f
                // First line: top-left to bottom-right
                drawLine(
                    color = iconColor,
                    start = Offset(center.x - crossSize, center.y - crossSize),
                    end = Offset(center.x + crossSize, center.y + crossSize),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // Second line: top-right to bottom-left
                drawLine(
                    color = iconColor,
                    start = Offset(center.x + crossSize, center.y - crossSize),
                    end = Offset(center.x - crossSize, center.y + crossSize),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            StatusIconType.EMPTY_CIRCLE -> {
                // Draw empty circle outline
                drawCircle(
                    color = iconColor,
                    radius = iconSize * 0.7f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
        }
    }
}

@Composable
private fun SignalBars(quality: PedalInfo.SignalQuality) {
    val activeColor = when (quality) {
        PedalInfo.SignalQuality.GOOD -> Theme.colors.optimal
        PedalInfo.SignalQuality.FAIR -> Theme.colors.attention
        PedalInfo.SignalQuality.POOR -> Theme.colors.problem
        PedalInfo.SignalQuality.UNKNOWN -> Theme.colors.dim
    }
    val inactiveColor = Theme.colors.muted

    val activeBars = when (quality) {
        PedalInfo.SignalQuality.GOOD -> 4
        PedalInfo.SignalQuality.FAIR -> 3
        PedalInfo.SignalQuality.POOR -> 1
        PedalInfo.SignalQuality.UNKNOWN -> 0
    }

    Canvas(
        modifier = Modifier
            .width(32.dp)
            .height(24.dp)
    ) {
        val barWidth = 6.dp.toPx()
        val spacing = 2.dp.toPx()
        val heights = listOf(6.dp.toPx(), 10.dp.toPx(), 14.dp.toPx(), 18.dp.toPx())

        heights.forEachIndexed { index, height ->
            val x = index * (barWidth + spacing)
            val y = size.height - height
            val color = if (index < activeBars) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

@Composable
private fun LiveMetricsSection(metrics: PedalingMetrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.live_data),
            color = Theme.colors.dim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.surface)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = stringResource(R.string.balance_lower),
                    value = "${(100 - metrics.balance).toInt()}/${metrics.balance.toInt()}",
                    unit = stringResource(R.string.lr)
                )
                MetricItem(
                    label = stringResource(R.string.te),
                    value = "${metrics.torqueEffAvg.toInt()}",
                    unit = "%"
                )
                MetricItem(
                    label = stringResource(R.string.ps),
                    value = "${metrics.pedalSmoothAvg.toInt()}",
                    unit = "%"
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.data_flowing),
            color = Theme.colors.optimal,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 10.sp
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                color = Theme.colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                color = Theme.colors.dim,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun SignalDetailsSection(pedalInfo: PedalInfo) {
    val excellentText = stringResource(R.string.excellent)
    val fairText = stringResource(R.string.fair)
    val poorText = stringResource(R.string.poor)
    val unknownText = stringResource(R.string.unknown)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.signal_details),
            color = Theme.colors.dim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Signal Quality
        DetailRow(
            label = stringResource(R.string.quality),
            value = when (pedalInfo.signalQuality) {
                PedalInfo.SignalQuality.GOOD -> excellentText
                PedalInfo.SignalQuality.FAIR -> fairText
                PedalInfo.SignalQuality.POOR -> poorText
                PedalInfo.SignalQuality.UNKNOWN -> unknownText
            },
            valueColor = when (pedalInfo.signalQuality) {
                PedalInfo.SignalQuality.GOOD -> Theme.colors.optimal
                PedalInfo.SignalQuality.FAIR -> Theme.colors.attention
                PedalInfo.SignalQuality.POOR -> Theme.colors.problem
                PedalInfo.SignalQuality.UNKNOWN -> Theme.colors.dim
            }
        )

        Divider()

        // Update Rate
        DetailRow(
            label = stringResource(R.string.update_rate),
            value = if (pedalInfo.updateFrequency > 0) {
                String.format(Locale.getDefault(), "%.1f Hz", pedalInfo.updateFrequency)
            } else {
                "—"
            },
            valueColor = when {
                pedalInfo.updateFrequency >= 1f -> Theme.colors.optimal
                pedalInfo.updateFrequency >= 0.5f -> Theme.colors.attention
                pedalInfo.updateFrequency > 0 -> Theme.colors.problem
                else -> Theme.colors.dim
            }
        )

        Divider()

        // Last Data
        DetailRow(
            label = stringResource(R.string.last_update),
            value = pedalInfo.getLastDataAgo()
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Theme.colors.text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Theme.colors.text,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TroubleshootingSection(pedalInfo: PedalInfo) {
    val tipStartRiding = stringResource(R.string.pedal_tip_start_riding)
    val tipEnsurePaired = stringResource(R.string.pedal_tip_ensure_paired)
    val tipCheckBattery = stringResource(R.string.pedal_tip_check_battery)
    val tipLowRate = stringResource(R.string.pedal_tip_low_rate)
    val tipMoveAway = stringResource(R.string.pedal_tip_move_away)
    val tipEnableDynamics = stringResource(R.string.pedal_tip_enable_dynamics)
    val tipNoPowerMeter = stringResource(R.string.pedal_tip_no_power_meter)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.troubleshooting),
            color = Theme.colors.dim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.attention.copy(alpha = 0.1f))
                .padding(10.dp)
        ) {
            Column {
                if (!pedalInfo.isConnected) {
                    TipItem(tipStartRiding)
                    TipItem(tipEnsurePaired)
                    TipItem(tipCheckBattery)
                } else if (!pedalInfo.hasPowerMeter) {
                    // Connected but no power meter detected
                    TipItem(tipNoPowerMeter)
                    TipItem(tipEnsurePaired)
                } else if (!pedalInfo.hasCyclingDynamics) {
                    // Power meter connected but no Cycling Dynamics
                    TipItem(tipEnableDynamics)
                } else {
                    // Connected with all features but poor signal
                    TipItem(tipLowRate)
                    TipItem(tipMoveAway)
                }
            }
        }
    }
}

@Composable
private fun TipItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            color = Theme.colors.attention,
            fontSize = 10.sp,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            color = Theme.colors.text,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SensorDiagnosticsSection(pedalInfo: PedalInfo) {
    val yesText = stringResource(R.string.yes)
    val noText = stringResource(R.string.no)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.sensor_diagnostics),
            color = Theme.colors.dim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Active sensor (if streaming)
        if (pedalInfo.isConnected) {
            DetailRow(
                label = stringResource(R.string.active_sensor),
                value = pedalInfo.modelName,
                valueColor = Theme.colors.optimal
            )
            Divider()
        }

        // Power Meter status
        DetailRow(
            label = stringResource(R.string.power_meter),
            value = if (pedalInfo.hasPowerMeter) yesText else noText,
            valueColor = if (pedalInfo.hasPowerMeter) Theme.colors.optimal else Theme.colors.problem
        )

        Divider()

        // Cycling Dynamics status
        DetailRow(
            label = stringResource(R.string.cycling_dynamics),
            value = if (pedalInfo.hasCyclingDynamics) yesText else noText,
            valueColor = if (pedalInfo.hasCyclingDynamics) Theme.colors.optimal else Theme.colors.problem
        )

        // Show detected sensors
        if (pedalInfo.sensors.isNotEmpty()) {
            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.detected_sensors),
                color = Theme.colors.dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            pedalInfo.sensors.forEach { sensor ->
                val isActive = sensor.id == pedalInfo.activeDeviceId && pedalInfo.isConnected
                SensorCard(sensor, isActive = isActive)
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Show tip if multiple sensors
            if (pedalInfo.sensors.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Theme.colors.attention.copy(alpha = 0.1f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.multiple_sensors_tip),
                        color = Theme.colors.text,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorCard(sensor: SensorInfo, isActive: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) Theme.colors.optimal.copy(alpha = 0.08f)
                else Theme.colors.surface
            )
    ) {
        // Green accent bar on the left for active sensor
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Theme.colors.optimal,
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(10.dp)
        ) {
            // Sensor name and connection type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sensor.name,
                    color = if (isActive) Theme.colors.optimal else Theme.colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sensor.connectionType.uppercase(Locale.getDefault()),
                    color = Theme.colors.dim,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(
                            color = Theme.colors.muted,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Capabilities row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CapabilityChip(
                    label = stringResource(R.string.balance_lower),
                    enabled = sensor.hasPowerBalance
                )
                CapabilityChip(
                    label = stringResource(R.string.te),
                    enabled = sensor.hasTorqueEffectiveness
                )
                CapabilityChip(
                    label = stringResource(R.string.ps),
                    enabled = sensor.hasPedalSmoothness
                )
            }
        }
    }
}

@Composable
private fun CapabilityChip(
    label: String,
    enabled: Boolean
) {
    val bgColor = if (enabled) Theme.colors.optimal.copy(alpha = 0.15f) else Theme.colors.muted
    val textColor = if (enabled) Theme.colors.optimal else Theme.colors.dim

    Text(
        text = label,
        color = textColor,
        fontSize = 10.sp,
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
