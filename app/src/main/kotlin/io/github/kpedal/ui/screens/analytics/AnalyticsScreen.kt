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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.data.models.TrendData
import io.github.kpedal.ui.components.charts.TrendChart
import io.github.kpedal.ui.theme.Theme
import kotlin.math.abs

/**
 * Analytics screen showing trends and progress.
 * Clean, informative layout for Karoo 3.
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
                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Theme.colors.dim,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
                    .padding(end = 12.dp)
            )
            Text(
                text = stringResource(R.string.analytics),
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        // Period tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendData.Period.entries.forEach { period ->
                PeriodTab(
                    label = period.label,
                    isSelected = trendData.period == period,
                    onClick = { onPeriodChange(period) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!trendData.hasData) {
            EmptyState()
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero stats
            HeroStats(
                rideCount = trendData.rideCount,
                progressScore = trendData.progressScore
            )

            // Metrics overview
            MetricsOverview(
                avgBalance = trendData.avgBalance,
                avgTE = trendData.avgTE,
                avgPS = trendData.avgPS
            )

            // Balance chart
            ChartSection(
                title = stringResource(R.string.balance),
                value = "${(100 - trendData.avgBalance).toInt()}/${trendData.avgBalance.toInt()}",
                valueColor = getBalanceColor(trendData.avgBalance)
            ) {
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

            // TE chart
            ChartSection(
                title = stringResource(R.string.torque_eff_short),
                value = "${trendData.avgTE.toInt()}%",
                valueColor = getTEColor(trendData.avgTE)
            ) {
                TrendChart(
                    data = trendData.teTrend,
                    lineColor = getTEColor(trendData.avgTE),
                    unit = "%",
                    minValue = 50f,
                    maxValue = 90f,
                    referenceValue = 75f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // PS chart
            ChartSection(
                title = stringResource(R.string.smoothness_short),
                value = "${trendData.avgPS.toInt()}%",
                valueColor = getPSColor(trendData.avgPS)
            ) {
                TrendChart(
                    data = trendData.psTrend,
                    lineColor = getPSColor(trendData.avgPS),
                    unit = "%",
                    minValue = 10f,
                    maxValue = 40f,
                    referenceValue = 20f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PeriodTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Theme.colors.optimal else Theme.colors.surface)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Theme.colors.background else Theme.colors.dim,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.no_ride_data),
                color = Theme.colors.dim,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.complete_rides_to_see),
                color = Theme.colors.muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HeroStats(
    rideCount: Int,
    progressScore: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rides count
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$rideCount",
                color = Theme.colors.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rides).uppercase(),
                color = Theme.colors.dim,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        }

        // Progress
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (progressText, progressColor) = when {
                progressScore > 5 -> "+$progressScore%" to Theme.colors.optimal
                progressScore < -5 -> "$progressScore%" to Theme.colors.problem
                else -> stringResource(R.string.stable) to Theme.colors.text
            }
            Text(
                text = progressText,
                color = progressColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.progress_label).uppercase(),
                color = Theme.colors.dim,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun MetricsOverview(
    avgBalance: Float,
    avgTE: Float,
    avgPS: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MetricItem(
            label = stringResource(R.string.balance_lower),
            value = "${(100 - avgBalance).toInt()}/${avgBalance.toInt()}",
            color = getBalanceColor(avgBalance)
        )
        MetricItem(
            label = stringResource(R.string.te),
            value = "${avgTE.toInt()}%",
            color = getTEColor(avgTE)
        )
        MetricItem(
            label = stringResource(R.string.ps),
            value = "${avgPS.toInt()}%",
            color = getPSColor(avgPS)
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
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
            fontSize = 9.sp
        )
    }
}

@Composable
private fun ChartSection(
    title: String,
    value: String,
    valueColor: Color,
    chart: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = Theme.colors.dim,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        chart()
    }
}

@Composable
private fun getBalanceColor(balance: Float): Color {
    val deviation = abs(balance - 50)
    return when {
        deviation <= 2.5f -> Theme.colors.optimal
        deviation <= 5f -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getTEColor(te: Float): Color {
    return when {
        te in 70f..80f -> Theme.colors.optimal
        te in 65f..85f -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getPSColor(ps: Float): Color {
    return when {
        ps >= 20f -> Theme.colors.optimal
        ps >= 15f -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}
