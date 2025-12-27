package io.github.kpedal.ui.screens.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator
import io.github.kpedal.ui.theme.Theme

// ════════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            text = title,
            color = Theme.colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
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

// ════════════════════════════════════════════════════════════════════════════
// 1. QUICK GLANCE - Status + Balance
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun QuickGlanceScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Quick Glance", onBack)
        Divider()

        // Status indicator (40%)
        Box(modifier = Modifier.weight(0.4f)) {
            StatusIndicator(metrics)
        }
        Divider()
        // Balance (60%)
        Box(modifier = Modifier.weight(0.6f)) {
            BalanceView(metrics)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 2. POWER + BALANCE - Power with L/R balance
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PowerBalanceScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Power + Balance", onBack)
        Divider()

        // Power (60%)
        Box(modifier = Modifier.weight(0.6f)) {
            PowerView(metrics)
        }
        Divider()
        // Balance (40%)
        Box(modifier = Modifier.weight(0.4f)) {
            BalanceView(metrics, compact = true)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 3. EFFICIENCY - TE + PS
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun EfficiencyScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Efficiency", onBack)
        Divider()

        // TE (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            TorqueEffView(metrics)
        }
        Divider()
        // PS (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            PedalSmoothView(metrics)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 4. FULL OVERVIEW - Balance + TE + PS
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun FullOverviewScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Full Overview", onBack)
        Divider()

        // Balance (33%)
        Box(modifier = Modifier.weight(1f)) {
            BalanceView(metrics, compact = true)
        }
        Divider()
        // TE (33%)
        Box(modifier = Modifier.weight(1f)) {
            TorqueEffView(metrics, compact = true)
        }
        Divider()
        // PS (33%)
        Box(modifier = Modifier.weight(1f)) {
            PedalSmoothView(metrics, compact = true)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 5. BALANCE TREND - Current, 3s, 10s smoothed balance
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun BalanceTrendScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Balance Trend", onBack)
        Divider()

        // Current Balance (33%)
        Box(modifier = Modifier.weight(1f)) {
            BalanceTrendRow("CURRENT", metrics.balanceLeft, metrics.balance)
        }
        Divider()
        // 3s Smoothed (33%)
        Box(modifier = Modifier.weight(1f)) {
            BalanceTrendRow("3s AVG", metrics.balance3sLeft, metrics.balance3s)
        }
        Divider()
        // 10s Smoothed (33%)
        Box(modifier = Modifier.weight(1f)) {
            BalanceTrendRow("10s AVG", metrics.balance10sLeft, metrics.balance10s)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 6. SINGLE BALANCE - Balance only (full screen)
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun SingleBalanceScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Balance", onBack)
        Divider()

        // Full screen balance
        Box(modifier = Modifier.weight(1f)) {
            BalanceView(metrics, large = true)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// VIEW COMPONENTS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun PowerView(metrics: PedalingMetrics) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.power),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${metrics.power}",
                color = Theme.colors.text,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.watt_symbol),
                color = Theme.colors.dim,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun BalanceTrendRow(label: String, left: Float, right: Float) {
    val leftInt = left.toInt()
    val rightInt = right.toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$leftInt",
                    color = Theme.colors.text,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$rightInt",
                    color = Theme.colors.text,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.left), color = Theme.colors.dim, fontSize = 12.sp)
            Text(text = stringResource(R.string.right), color = Theme.colors.dim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatusIndicator(metrics: PedalingMetrics) {
    // Compute directly - metrics is already a stable data class
    val allOptimal = StatusCalculator.allOptimal(metrics)
    val issueCount = StatusCalculator.countIssues(metrics)

    val bgColor = if (allOptimal) {
        Theme.colors.optimal.copy(alpha = 0.08f)
    } else {
        Theme.colors.attention.copy(alpha = 0.08f)
    }

    val statusColor = when {
        allOptimal -> Theme.colors.optimal
        issueCount >= 2 -> Theme.colors.problem
        else -> Theme.colors.attention
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        val allGoodText = stringResource(R.string.all_good)
        val issueText = if (issueCount > 1) {
            stringResource(R.string.issues_count, issueCount)
        } else {
            stringResource(R.string.issue_count, issueCount)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (allOptimal) "✓" else "!",
                color = statusColor,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (allOptimal) allGoodText else issueText,
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BalanceView(
    metrics: PedalingMetrics,
    compact: Boolean = false,
    large: Boolean = false
) {
    // Compute directly - simple int conversions don't need memoization
    val left = metrics.balanceLeft.toInt()
    val right = metrics.balance.toInt()
    val status = StatusCalculator.balanceStatus(metrics.balance)
    val statusColor = Color(StatusCalculator.statusColor(status))

    val fontSize = when {
        large -> 72.sp
        compact -> 36.sp
        else -> 52.sp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.balance),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Values
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
                    text = "$left",
                    color = if (left > 52 && status != StatusCalculator.Status.OPTIMAL) statusColor else Theme.colors.text,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$right",
                    color = if (right > 52 && status != StatusCalculator.Status.OPTIMAL) statusColor else Theme.colors.text,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Balance bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (large) 12.dp else 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF222222))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(left.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(if (left > 52) statusColor else Theme.colors.dim)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Theme.colors.text)
                )
                Box(
                    modifier = Modifier
                        .weight(right.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(if (right > 52) statusColor else Theme.colors.dim)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // L/R labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.left), color = Theme.colors.dim, fontSize = 12.sp)
            Text(text = stringResource(R.string.right), color = Theme.colors.dim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TorqueEffView(metrics: PedalingMetrics, compact: Boolean = false) {
    val avg = metrics.torqueEffAvg.toInt()
    val avgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)
    val leftStatus = StatusCalculator.teStatus(metrics.torqueEffLeft)
    val rightStatus = StatusCalculator.teStatus(metrics.torqueEffRight)
    val leftValue = metrics.torqueEffLeft.toInt()
    val rightValue = metrics.torqueEffRight.toInt()

    val fontSize = if (compact) 32.sp else 48.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.te),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$avg%",
                color = Color(StatusCalculator.statusColor(avgStatus)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
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
                    text = "$leftValue",
                    color = Color(StatusCalculator.statusColor(leftStatus)),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.left), color = Theme.colors.dim, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Theme.colors.muted)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$rightValue",
                    color = Color(StatusCalculator.statusColor(rightStatus)),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.right), color = Theme.colors.dim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PedalSmoothView(metrics: PedalingMetrics, compact: Boolean = false) {
    val avg = metrics.pedalSmoothAvg.toInt()
    val avgStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)
    val leftStatus = StatusCalculator.psStatus(metrics.pedalSmoothLeft)
    val rightStatus = StatusCalculator.psStatus(metrics.pedalSmoothRight)
    val leftValue = metrics.pedalSmoothLeft.toInt()
    val rightValue = metrics.pedalSmoothRight.toInt()

    val fontSize = if (compact) 32.sp else 48.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.ps),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$avg%",
                color = Color(StatusCalculator.statusColor(avgStatus)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
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
                    text = "$leftValue",
                    color = Color(StatusCalculator.statusColor(leftStatus)),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.left), color = Theme.colors.dim, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Theme.colors.muted)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$rightValue",
                    color = Color(StatusCalculator.statusColor(rightStatus)),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.right), color = Theme.colors.dim, fontSize = 12.sp)
            }
        }
    }
}

