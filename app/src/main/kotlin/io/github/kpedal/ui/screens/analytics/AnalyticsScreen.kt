package io.github.kpedal.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.data.models.TrendData
import io.github.kpedal.ui.components.charts.TrendChart
import io.github.kpedal.ui.theme.Theme
import kotlin.math.abs

/**
 * Analytics screen showing trends and progress.
 */
@Composable
fun AnalyticsScreen(
    trendData: TrendData,
    onBack: () -> Unit,
    onPeriodChange: (TrendData.Period) -> Unit
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                Text(
                    text = "Analytics",
                    color = Theme.colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Period selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendData.Period.entries.forEach { period ->
                    PeriodChip(
                        label = period.label,
                        isSelected = trendData.period == period,
                        onClick = { onPeriodChange(period) }
                    )
                }
            }
        }

        Divider()

        if (!trendData.hasData) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No ride data",
                        color = Theme.colors.dim,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete some rides to see trends",
                        color = Theme.colors.muted,
                        fontSize = 12.sp
                    )
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Summary
            ProgressCard(
                progressScore = trendData.progressScore,
                rideCount = trendData.rideCount,
                avgBalance = trendData.avgBalance
            )

            // Balance Trend
            ChartCard(title = "Balance Trend") {
                TrendChart(
                    data = trendData.balanceTrend,
                    lineColor = getBalanceColor(trendData.avgBalance),
                    unit = "%",
                    minValue = 40f,
                    maxValue = 60f,
                    referenceValue = 50f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // TE Trend
            ChartCard(title = "Torque Effectiveness") {
                TrendChart(
                    data = trendData.teTrend,
                    lineColor = getTEColor(trendData.avgTE),
                    unit = "%",
                    minValue = 0f,
                    maxValue = 100f,
                    referenceValue = 75f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // PS Trend
            ChartCard(title = "Pedal Smoothness") {
                TrendChart(
                    data = trendData.psTrend,
                    lineColor = getPSColor(trendData.avgPS),
                    unit = "%",
                    minValue = 0f,
                    maxValue = 50f,
                    referenceValue = 20f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Averages Summary
            AveragesCard(
                avgBalance = trendData.avgBalance,
                avgTE = trendData.avgTE,
                avgPS = trendData.avgPS
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgressCard(
    progressScore: Int,
    rideCount: Int,
    avgBalance: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = "Progress",
            value = when {
                progressScore > 5 -> "+$progressScore%"
                progressScore < -5 -> "$progressScore%"
                else -> "Stable"
            },
            valueColor = when {
                progressScore > 5 -> Theme.colors.optimal
                progressScore < -5 -> Theme.colors.problem
                else -> Theme.colors.text
            }
        )
        StatItem(
            label = "Rides",
            value = rideCount.toString(),
            valueColor = Theme.colors.text
        )
        StatItem(
            label = "Avg Balance",
            value = "L${(100 - avgBalance).toInt()}/R${avgBalance.toInt()}",
            valueColor = getBalanceColor(avgBalance)
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = Theme.colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun AveragesCard(
    avgBalance: Float,
    avgTE: Float,
    avgPS: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        Text(
            text = "Period Averages",
            color = Theme.colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AverageItem(
                label = "Balance",
                value = "${avgBalance.toInt()}% R",
                color = getBalanceColor(avgBalance)
            )
            AverageItem(
                label = "TE",
                value = "${avgTE.toInt()}%",
                color = getTEColor(avgTE)
            )
            AverageItem(
                label = "PS",
                value = "${avgPS.toInt()}%",
                color = getPSColor(avgPS)
            )
        }
    }
}

@Composable
private fun AverageItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (isSelected) Theme.colors.background else Theme.colors.dim,
        fontSize = 10.sp,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Theme.colors.optimal else Theme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
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
private fun getBalanceColor(balance: Float): androidx.compose.ui.graphics.Color {
    val deviation = abs(balance - 50)
    return when {
        deviation <= 2.5f -> Theme.colors.optimal
        deviation <= 5f -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getTEColor(te: Float): androidx.compose.ui.graphics.Color {
    return when {
        te in 70f..80f -> Theme.colors.optimal
        te in 65f..85f -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getPSColor(ps: Float): androidx.compose.ui.graphics.Color {
    return when {
        ps >= 20f -> Theme.colors.optimal
        ps >= 15f -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}
