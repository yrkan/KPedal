package io.github.kpedal.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.SensorStreamState
import io.github.kpedal.engine.StatusCalculator
import io.github.kpedal.ui.theme.Theme
import kotlin.math.roundToInt

/**
 * Live ride data displayed during an active ride.
 */
@Immutable
data class LiveRideData(
    val duration: String = "0:00:00",
    val balanceLeft: Int = 50,
    val balanceRight: Int = 50,
    val teLeft: Int = 0,
    val teRight: Int = 0,
    val psLeft: Int = 0,
    val psRight: Int = 0,
    val zoneOptimal: Int = 0,
    val zoneAttention: Int = 0,
    val zoneProblem: Int = 0,
    // Zone time in minutes for display
    val zoneOptimalMin: Int = 0,
    val zoneAttentionMin: Int = 0,
    val zoneProblemMin: Int = 0,
    // Zone time in milliseconds (for Zone Timer DataType mm:ss format)
    val timeOptimalMs: Long = 0L,
    val timeAttentionMs: Long = 0L,
    val timeProblemMs: Long = 0L,
    // Trend indicators (-1 = worse, 0 = stable, 1 = better)
    val balanceTrend: Int = 0,
    val teTrend: Int = 0,
    val psTrend: Int = 0,
    // Overall score (0-100)
    val score: Int = 0,
    // Extended metrics
    val powerAvg: Int = 0,
    val powerMax: Int = 0,
    val cadenceAvg: Int = 0,
    val heartRateAvg: Int = 0,
    val heartRateMax: Int = 0,
    val speedAvgKmh: Float = 0f,
    val distanceKm: Float = 0f,
    // Pro cyclist metrics - Climbing
    val elevationGain: Int = 0,      // meters
    val elevationLoss: Int = 0,      // meters
    val gradeAvg: Float = 0f,        // %
    val gradeMax: Float = 0f,        // %
    // Pro cyclist metrics - Power analytics
    val normalizedPower: Int = 0,    // NP in watts
    val energyKj: Int = 0,           // kilojoules
    // Whether we have received real pedal data
    val hasData: Boolean = false
)

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Theme.colors.divider)
    )
}

private enum class SaveStatus {
    Idle, Saved, Failed
}

/**
 * Returns trend arrow symbol based on trend value.
 * @param trend -1 = worse, 0 = stable, 1 = better
 */
private fun trendArrow(trend: Int): String = when (trend) {
    1 -> "↑"
    -1 -> "↓"
    else -> ""
}

/**
 * LiveScreen - Real-time ride data display
 *
 * When a ride is active (liveData.hasData = true), shows accumulated ride statistics.
 * When no ride is active but metrics are available, shows instant real-time values.
 */
@Composable
fun LiveScreen(
    liveData: LiveRideData,
    metrics: PedalingMetrics = PedalingMetrics(),
    sensorState: SensorStreamState = SensorStreamState.Idle,
    onBack: () -> Unit,
    onSave: () -> Boolean = { false }
) {
    var saveStatus by remember { mutableStateOf(SaveStatus.Idle) }

    // Use instant metrics when no ride is active but sensor data is available
    val displayData = if (liveData.hasData) {
        liveData
    } else if (metrics.hasData) {
        // Create LiveRideData from instant metrics
        LiveRideData(
            duration = "--:--",  // No duration without ride
            balanceLeft = metrics.balanceLeft.roundToInt(),
            balanceRight = metrics.balance.roundToInt(),
            teLeft = metrics.torqueEffLeft.roundToInt(),
            teRight = metrics.torqueEffRight.roundToInt(),
            psLeft = metrics.pedalSmoothLeft.roundToInt(),
            psRight = metrics.pedalSmoothRight.roundToInt(),
            zoneOptimal = 0,
            zoneAttention = 0,
            zoneProblem = 0,
            balanceTrend = 0,
            teTrend = 0,
            psTrend = 0,
            score = 0,
            hasData = true  // We have instant data to show
        )
    } else {
        liveData  // No data available
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        // Header (with top padding for Karoo status bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "←",
                    color = Theme.colors.dim,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBack() }
                        .padding(end = 12.dp)
                )
                // Pulsing green dot (like our logo)
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(Theme.colors.optimal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.live),
                    color = Theme.colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Score with color based on value
                val scoreColor = when {
                    displayData.score >= 80 -> Theme.colors.optimal
                    displayData.score >= 50 -> Theme.colors.attention
                    displayData.score > 0 -> Theme.colors.problem
                    else -> Theme.colors.dim
                }
                if (displayData.score > 0) {
                    Text(
                        text = "${displayData.score}",
                        color = scoreColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = displayData.duration,
                    color = Theme.colors.dim,
                    fontSize = 12.sp
                )
                // Save icon (compact)
                when (saveStatus) {
                    SaveStatus.Idle -> {
                        Text(
                            text = "↓",
                            color = Theme.colors.optimal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                if (onSave()) {
                                    saveStatus = SaveStatus.Saved
                                } else {
                                    saveStatus = SaveStatus.Failed
                                }
                            }
                        )
                    }
                    SaveStatus.Saved -> {
                        Text(
                            text = "✓",
                            color = Theme.colors.dim,
                            fontSize = 14.sp
                        )
                    }
                    SaveStatus.Failed -> {
                        Text(
                            text = "!",
                            color = Theme.colors.problem,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Divider()

        // Sensor status bar - shows connection and data state
        val (statusText, statusColor, statusBgColor) = when {
            sensorState is SensorStreamState.Streaming && metrics.hasData -> Triple(
                stringResource(R.string.sensor_receiving_data),
                Theme.colors.optimal,
                Color(0xFF1A2E1A)  // Dark green
            )
            sensorState is SensorStreamState.Streaming -> Triple(
                stringResource(R.string.sensor_connected_waiting),
                Theme.colors.attention,
                Color(0xFF2E2E1A)  // Dark yellow
            )
            sensorState is SensorStreamState.Searching -> Triple(
                stringResource(R.string.sensor_searching),
                Theme.colors.dim,
                Color(0xFF1A1A1A)  // Dark gray
            )
            sensorState is SensorStreamState.Disconnected -> Triple(
                stringResource(R.string.sensor_disconnected),
                Theme.colors.problem,
                Color(0xFF2E1A1A)  // Dark red
            )
            sensorState is SensorStreamState.NotAvailable -> Triple(
                stringResource(R.string.sensor_not_available),
                Theme.colors.problem,
                Color(0xFF2E1A1A)  // Dark red
            )
            else -> Triple(
                stringResource(R.string.sensor_idle),
                Theme.colors.dim,
                Color(0xFF1A1A1A)  // Dark gray
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusBgColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp
            )
        }

        // BALANCE section (weight 1)
        Box(modifier = Modifier.weight(1f)) {
            BalanceSection(displayData)
        }

        Divider()

        // TE & PS grid (weight 1)
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f)) {
                MetricSection(
                    label = "TE",
                    left = displayData.teLeft,
                    right = displayData.teRight,
                    trend = displayData.teTrend,
                    hasData = displayData.hasData,
                    statusFn = { StatusCalculator.teStatus(it.toFloat()) }
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Theme.colors.divider)
            )
            Box(modifier = Modifier.weight(1f)) {
                MetricSection(
                    label = "PS",
                    left = displayData.psLeft,
                    right = displayData.psRight,
                    trend = displayData.psTrend,
                    hasData = displayData.hasData,
                    statusFn = { StatusCalculator.psStatus(it.toFloat()) }
                )
            }
        }

        Divider()

        // TIME IN ZONE section (weight 1)
        Box(modifier = Modifier.weight(1f)) {
            TimeInZoneSection(displayData)
        }
    }
}

@Composable
private fun BalanceSection(liveData: LiveRideData) {
    val trendText = trendArrow(liveData.balanceTrend)
    val trendColor = when (liveData.balanceTrend) {
        1 -> Theme.colors.optimal
        -1 -> Theme.colors.problem
        else -> Theme.colors.dim
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.balance),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (trendText.isNotEmpty()) {
                Text(
                    text = " $trendText",
                    color = trendColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Balance numbers with L/R labels inline
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "${liveData.balanceLeft}",
                    color = Theme.colors.text,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.left),
                    color = Theme.colors.dim,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${liveData.balanceRight}",
                    color = Theme.colors.text,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.right),
                    color = Theme.colors.dim,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Balance bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF222222))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(liveData.balanceLeft.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(Theme.colors.dim)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Theme.colors.text)
                )
                Box(
                    modifier = Modifier
                        .weight(liveData.balanceRight.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(Theme.colors.dim)
                )
            }
        }
    }
}

@Composable
private fun MetricSection(
    label: String,
    left: Int,
    right: Int,
    trend: Int = 0,
    hasData: Boolean = true,
    statusFn: (Int) -> StatusCalculator.Status
) {
    val trendText = if (hasData) trendArrow(trend) else ""
    val trendColor = when (trend) {
        1 -> Theme.colors.optimal
        -1 -> Theme.colors.problem
        else -> Theme.colors.dim
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (trendText.isNotEmpty()) {
                Text(
                    text = " $trendText",
                    color = trendColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (hasData) "$left" else "--",
                    color = if (hasData) Color(StatusCalculator.statusColor(statusFn(left))) else Theme.colors.dim,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.left), color = Theme.colors.dim, fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (hasData) "$right" else "--",
                    color = if (hasData) Color(StatusCalculator.statusColor(statusFn(right))) else Theme.colors.dim,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.right), color = Theme.colors.dim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TimeInZoneSection(liveData: LiveRideData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.time_in_zone),
            color = Theme.colors.dim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${liveData.zoneOptimal}%",
                        color = Theme.colors.optimal,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (liveData.zoneOptimalMin > 0) {
                        Text(
                            text = "${liveData.zoneOptimalMin}m",
                            color = Theme.colors.dim,
                            fontSize = 10.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${liveData.zoneAttention}%",
                        color = Theme.colors.attention,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (liveData.zoneAttentionMin > 0) {
                        Text(
                            text = "${liveData.zoneAttentionMin}m",
                            color = Theme.colors.dim,
                            fontSize = 10.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${liveData.zoneProblem}%",
                        color = Theme.colors.problem,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (liveData.zoneProblemMin > 0) {
                        Text(
                            text = "${liveData.zoneProblemMin}m",
                            color = Theme.colors.dim,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val total = (liveData.zoneOptimal + liveData.zoneAttention + liveData.zoneProblem)
                .coerceAtLeast(1)

            if (liveData.zoneOptimal > 0) {
                Box(
                    modifier = Modifier
                        .weight(liveData.zoneOptimal.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.optimal)
                )
            }
            if (liveData.zoneAttention > 0) {
                Box(
                    modifier = Modifier
                        .weight(liveData.zoneAttention.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.attention)
                )
            }
            if (liveData.zoneProblem > 0) {
                Box(
                    modifier = Modifier
                        .weight(liveData.zoneProblem.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.problem)
                )
            }
            if (total <= 1 && liveData.zoneOptimal == 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Theme.colors.muted)
                )
            }
        }
    }
}
