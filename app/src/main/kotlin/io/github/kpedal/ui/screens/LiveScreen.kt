package io.github.kpedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.engine.StatusCalculator
import io.github.kpedal.ui.theme.Theme

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
 */
@Composable
fun LiveScreen(
    liveData: LiveRideData,
    onBack: () -> Unit,
    onSave: () -> Boolean = { false },
    onNavigateToDrills: () -> Unit = {}
) {
    var saveStatus by remember { mutableStateOf(SaveStatus.Idle) }

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
                // Tabs: Live | Drills
                Text(
                    text = "Live",
                    color = Theme.colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " · ",
                    color = Theme.colors.dim,
                    fontSize = 14.sp
                )
                Text(
                    text = "Drills",
                    color = Theme.colors.dim,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigateToDrills() }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score with color based on value
                val scoreColor = when {
                    liveData.score >= 80 -> Theme.colors.optimal
                    liveData.score >= 50 -> Theme.colors.attention
                    liveData.score > 0 -> Theme.colors.problem
                    else -> Theme.colors.dim
                }
                if (liveData.score > 0) {
                    Text(
                        text = "${liveData.score}",
                        color = scoreColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = liveData.duration,
                    color = Theme.colors.dim,
                    fontSize = 12.sp
                )
                // Save button
                when (saveStatus) {
                    SaveStatus.Idle -> {
                        Text(
                            text = "Save",
                            color = Theme.colors.optimal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
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
                            text = "Saved ✓",
                            color = Theme.colors.dim,
                            fontSize = 12.sp
                        )
                    }
                    SaveStatus.Failed -> {
                        Text(
                            text = "No data",
                            color = Theme.colors.problem,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Divider()

        if (!liveData.hasData) {
            // Show waiting message when no pedal data yet
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Waiting for pedal data...",
                        color = Theme.colors.dim,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start riding or check pedal connection",
                        color = Theme.colors.muted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // BALANCE section (weight 1)
            Box(modifier = Modifier.weight(1f)) {
                BalanceSection(liveData)
            }

            Divider()

            // TE & PS grid (weight 1)
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f)) {
                    MetricSection("TE", liveData.teLeft, liveData.teRight, liveData.teTrend) {
                        StatusCalculator.teStatus(it.toFloat())
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Theme.colors.divider)
                )
                Box(modifier = Modifier.weight(1f)) {
                    MetricSection("PS", liveData.psLeft, liveData.psRight, liveData.psTrend) {
                        StatusCalculator.psStatus(it.toFloat())
                    }
                }
            }

            Divider()

            // TIME IN ZONE section (weight 1)
            Box(modifier = Modifier.weight(1f)) {
                TimeInZoneSection(liveData)
            }
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
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BALANCE",
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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${liveData.balanceLeft}",
                    color = Theme.colors.text,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${liveData.balanceRight}",
                    color = Theme.colors.text,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Balance bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
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

        Spacer(modifier = Modifier.height(4.dp))

        // L/R labels at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "L", color = Theme.colors.dim, fontSize = 12.sp)
            Text(text = "R", color = Theme.colors.dim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MetricSection(
    label: String,
    left: Int,
    right: Int,
    trend: Int = 0,
    statusFn: (Int) -> StatusCalculator.Status
) {
    val leftStatus = statusFn(left)
    val rightStatus = statusFn(right)
    val trendText = trendArrow(trend)
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
                    text = "$left",
                    color = Color(StatusCalculator.statusColor(leftStatus)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "L", color = Theme.colors.dim, fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$right",
                    color = Color(StatusCalculator.statusColor(rightStatus)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "R", color = Theme.colors.dim, fontSize = 12.sp)
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
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Text(
            text = "TIME IN ZONE",
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
                    Text(
                        text = "Opt",
                        color = Theme.colors.dim,
                        fontSize = 10.sp
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
                    Text(
                        text = "Att",
                        color = Theme.colors.dim,
                        fontSize = 10.sp
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
                    Text(
                        text = "Prob",
                        color = Theme.colors.dim,
                        fontSize = 10.sp
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
