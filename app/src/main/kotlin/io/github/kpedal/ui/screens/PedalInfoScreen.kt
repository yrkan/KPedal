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
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.ui.theme.Theme

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

            // Troubleshooting (when not connected or poor signal)
            if (!pedalInfo.isConnected || pedalInfo.signalQuality == PedalInfo.SignalQuality.POOR) {
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

@Composable
private fun ConnectionStatusCard(pedalInfo: PedalInfo) {
    val statusColor = if (pedalInfo.isConnected) Theme.colors.optimal else Theme.colors.problem
    val bgColor = statusColor.copy(alpha = 0.12f)
    val connectedText = stringResource(R.string.connected)
    val disconnectedText = stringResource(R.string.disconnected)

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
            // Status indicator with pulse effect
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (pedalInfo.isConnected) "✓" else "×",
                    color = Theme.colors.background,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pedalInfo.isConnected) connectedText else disconnectedText,
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

            // Signal bars (WiFi-style)
            if (pedalInfo.isConnected) {
                SignalBars(pedalInfo.signalQuality)
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
                String.format("%.1f Hz", pedalInfo.updateFrequency)
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
            fontSize = 13.sp
        )
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
                } else {
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
