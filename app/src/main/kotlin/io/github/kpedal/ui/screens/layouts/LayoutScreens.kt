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
            BalanceTrendRow(stringResource(R.string.label_current), metrics.balanceLeft, metrics.balance)
        }
        Divider()
        // 3s Smoothed (33%)
        Box(modifier = Modifier.weight(1f)) {
            BalanceTrendRow(stringResource(R.string.label_3s_avg), metrics.balance3sLeft, metrics.balance3s)
        }
        Divider()
        // 10s Smoothed (33%)
        Box(modifier = Modifier.weight(1f)) {
            BalanceTrendRow(stringResource(R.string.label_10s_avg), metrics.balance10sLeft, metrics.balance10s)
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

// ════════════════════════════════════════════════════════════════════════════
// 7. LIVE LAYOUT - Real-time overview
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun LiveLayoutScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Live", onBack)
        Divider()

        // Power + Cadence (30%)
        Box(modifier = Modifier.weight(0.3f)) {
            LivePowerCadenceView(metrics)
        }
        Divider()
        // Balance (35%)
        Box(modifier = Modifier.weight(0.35f)) {
            BalanceView(metrics, compact = true)
        }
        Divider()
        // TE + PS (35%)
        Box(modifier = Modifier.weight(0.35f)) {
            LiveEfficiencyRow(metrics)
        }
    }
}

@Composable
private fun LivePowerCadenceView(metrics: PedalingMetrics) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${metrics.power}",
                color = Theme.colors.text,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.watt_symbol),
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(50.dp)
                .background(Theme.colors.muted)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${metrics.cadence}",
                color = Theme.colors.text,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rpm),
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LiveEfficiencyRow(metrics: PedalingMetrics) {
    val teAvg = metrics.torqueEffAvg.toInt()
    val psAvg = metrics.pedalSmoothAvg.toInt()
    val teStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)
    val psStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$teAvg%",
                color = Color(StatusCalculator.statusColor(teStatus)),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.te),
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Theme.colors.muted)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$psAvg%",
                color = Color(StatusCalculator.statusColor(psStatus)),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.ps),
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 8. PEDALING SCORE - Score with breakdown
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PedalingScoreScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    val balanceScore = StatusCalculator.calculateBalanceScore(metrics.balance.toInt())
    val efficiencyScore = StatusCalculator.calculateEfficiencyScore(
        metrics.torqueEffLeft.toInt(),
        metrics.torqueEffRight.toInt(),
        metrics.pedalSmoothLeft.toInt(),
        metrics.pedalSmoothRight.toInt()
    )
    // Use efficiency as proxy for consistency in real-time
    val overallScore = (balanceScore * 0.5 + efficiencyScore * 0.5).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Pedaling Score", onBack)
        Divider()

        // Score (50%)
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ScoreDisplay(overallScore)
        }
        Divider()
        // Breakdown (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            ScoreBreakdown(balanceScore, efficiencyScore)
        }
    }
}

@Composable
private fun ScoreDisplay(score: Int) {
    val color = when {
        score >= 90 -> Theme.colors.optimal
        score >= 70 -> Theme.colors.attention
        else -> Theme.colors.problem
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$score",
            color = color,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.score),
            color = Theme.colors.dim,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ScoreBreakdown(balanceScore: Int, efficiencyScore: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        ScoreRow(stringResource(R.string.balance), balanceScore)
        ScoreRow(stringResource(R.string.efficiency), efficiencyScore)
    }
}

@Composable
private fun ScoreRow(label: String, score: Int) {
    val color = when {
        score >= 90 -> Theme.colors.optimal
        score >= 70 -> Theme.colors.attention
        else -> Theme.colors.problem
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 14.sp
        )
        Text(
            text = "$score",
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 9. FATIGUE INDICATOR - Efficiency trend
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun FatigueIndicatorScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    val teAvg = metrics.torqueEffAvg.toInt()
    val psAvg = metrics.pedalSmoothAvg.toInt()
    val combinedEff = (teAvg + psAvg) / 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Fatigue Indicator", onBack)
        Divider()

        // Main indicator
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FatigueDisplay(combinedEff)
        }
    }
}

@Composable
private fun FatigueDisplay(efficiency: Int) {
    val fatigueLevel = when {
        efficiency >= 75 -> stringResource(R.string.fatigue_fresh)
        efficiency >= 60 -> stringResource(R.string.fatigue_good)
        efficiency >= 45 -> stringResource(R.string.fatigue_moderate)
        else -> stringResource(R.string.fatigue_fatigued)
    }

    val color = when {
        efficiency >= 75 -> Theme.colors.optimal
        efficiency >= 60 -> Theme.colors.attention
        else -> Theme.colors.problem
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = fatigueLevel,
            color = color,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$efficiency%",
            color = Theme.colors.text,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.efficiency),
            color = Theme.colors.dim,
            fontSize = 12.sp
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 10. CADENCE BALANCE - Cadence with balance
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun CadenceBalanceScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Cadence + Balance", onBack)
        Divider()

        // Cadence (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            CadenceView(metrics)
        }
        Divider()
        // Balance (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            BalanceView(metrics, compact = true)
        }
    }
}

@Composable
private fun CadenceView(metrics: PedalingMetrics) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.cadence),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${metrics.cadence}",
                color = Theme.colors.text,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rpm),
                color = Theme.colors.dim,
                fontSize = 14.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 11. DELTA AVERAGE - Balance deviation from 50%
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun DeltaAverageScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    val delta = metrics.balance - 50f
    val delta3s = metrics.balance3s - 50f
    val delta10s = metrics.balance10s - 50f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Delta Average", onBack)
        Divider()

        // Current delta (33%)
        Box(modifier = Modifier.weight(1f)) {
            DeltaRow(stringResource(R.string.label_current), delta)
        }
        Divider()
        // 3s delta (33%)
        Box(modifier = Modifier.weight(1f)) {
            DeltaRow(stringResource(R.string.label_3s_avg), delta3s)
        }
        Divider()
        // 10s delta (33%)
        Box(modifier = Modifier.weight(1f)) {
            DeltaRow(stringResource(R.string.label_10s_avg), delta10s)
        }
    }
}

@Composable
private fun DeltaRow(label: String, delta: Float) {
    val deltaInt = kotlin.math.abs(delta).toInt()
    val side = if (delta > 0) "R" else if (delta < 0) "L" else ""
    val status = StatusCalculator.balanceStatus(50f + kotlin.math.abs(delta))
    val color = Color(StatusCalculator.statusColor(status))

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (delta != 0f) (if (delta > 0) "+" else "-") else "",
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$deltaInt",
                    color = color,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                if (side.isNotEmpty()) {
                    Text(
                        text = side,
                        color = Theme.colors.dim,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 12. LEFT LEG - Focus on left leg metrics
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun LeftLegScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Left Leg", onBack)
        Divider()

        // Left Balance (40%)
        Box(modifier = Modifier.weight(0.4f)) {
            SingleLegBalance(metrics.balanceLeft.toInt(), stringResource(R.string.left))
        }
        Divider()
        // Left TE (30%)
        Box(modifier = Modifier.weight(0.3f)) {
            SingleLegMetric(
                metrics.torqueEffLeft.toInt(),
                stringResource(R.string.te),
                StatusCalculator.teStatus(metrics.torqueEffLeft)
            )
        }
        Divider()
        // Left PS (30%)
        Box(modifier = Modifier.weight(0.3f)) {
            SingleLegMetric(
                metrics.pedalSmoothLeft.toInt(),
                stringResource(R.string.ps),
                StatusCalculator.psStatus(metrics.pedalSmoothLeft)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 13. RIGHT LEG - Focus on right leg metrics
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun RightLegScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Right Leg", onBack)
        Divider()

        // Right Balance (40%)
        Box(modifier = Modifier.weight(0.4f)) {
            SingleLegBalance(metrics.balance.toInt(), stringResource(R.string.right))
        }
        Divider()
        // Right TE (30%)
        Box(modifier = Modifier.weight(0.3f)) {
            SingleLegMetric(
                metrics.torqueEffRight.toInt(),
                stringResource(R.string.te),
                StatusCalculator.teStatus(metrics.torqueEffRight)
            )
        }
        Divider()
        // Right PS (30%)
        Box(modifier = Modifier.weight(0.3f)) {
            SingleLegMetric(
                metrics.pedalSmoothRight.toInt(),
                stringResource(R.string.ps),
                StatusCalculator.psStatus(metrics.pedalSmoothRight)
            )
        }
    }
}

@Composable
private fun SingleLegBalance(value: Int, label: String) {
    val status = StatusCalculator.balanceStatus(value.toFloat())
    val color = Color(StatusCalculator.statusColor(status))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$value%",
                color = color,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = Theme.colors.dim,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SingleLegMetric(value: Int, label: String, status: StatusCalculator.Status) {
    val color = Color(StatusCalculator.statusColor(status))

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "$value%",
            color = color,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 14. CLIMBING MODE - Grade + Power + Balance
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun ClimbingModeScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Climbing Mode", onBack)
        Divider()

        // Grade + Power (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            ClimbingTopView(metrics)
        }
        Divider()
        // Balance (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            BalanceView(metrics, compact = true)
        }
    }
}

@Composable
private fun ClimbingTopView(metrics: PedalingMetrics) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", metrics.grade),
                color = if (metrics.grade > 0) Theme.colors.attention else Theme.colors.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "%",
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(50.dp)
                .background(Theme.colors.muted)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${metrics.power}",
                color = Theme.colors.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.watt_symbol),
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 15. SYMMETRY INDEX - Balance as symmetry metric
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun SymmetryIndexScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    // Symmetry index: 100% = perfect balance (50/50)
    val symmetry = 100 - kotlin.math.abs(metrics.balance - 50) * 2
    val symmetry3s = 100 - kotlin.math.abs(metrics.balance3s - 50) * 2
    val symmetry10s = 100 - kotlin.math.abs(metrics.balance10s - 50) * 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Symmetry Index", onBack)
        Divider()

        // Current (40%)
        Box(modifier = Modifier.weight(0.4f)) {
            SymmetryDisplay(symmetry.toInt(), stringResource(R.string.label_current))
        }
        Divider()
        // Trend (60%)
        Box(modifier = Modifier.weight(0.6f)) {
            SymmetryTrend(symmetry3s.toInt(), symmetry10s.toInt())
        }
    }
}

@Composable
private fun SymmetryDisplay(symmetry: Int, label: String) {
    val color = when {
        symmetry >= 95 -> Theme.colors.optimal
        symmetry >= 90 -> Theme.colors.attention
        else -> Theme.colors.problem
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$symmetry%",
                color = color,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SymmetryTrend(sym3s: Int, sym10s: Int) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$sym3s%",
                color = Theme.colors.text,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "3s",
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Theme.colors.muted)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$sym10s%",
                color = Theme.colors.text,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "10s",
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 16. HR EFFICIENCY - Heart rate with efficiency
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun HrEfficiencyScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("HR + Efficiency", onBack)
        Divider()

        // HR (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            HeartRateView(metrics)
        }
        Divider()
        // Efficiency (50%)
        Box(modifier = Modifier.weight(0.5f)) {
            LiveEfficiencyRow(metrics)
        }
    }
}

@Composable
private fun HeartRateView(metrics: PedalingMetrics) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.heart_rate),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (metrics.heartRate > 0) "${metrics.heartRate}" else "--",
                color = if (metrics.heartRate > 0) Theme.colors.problem else Theme.colors.text,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.bpm),
                color = Theme.colors.dim,
                fontSize = 14.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 17. POWER FOCUS - Power dominant view
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PowerFocusScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Power Focus", onBack)
        Divider()

        // Power large (60%)
        Box(modifier = Modifier.weight(0.6f)) {
            PowerView(metrics)
        }
        Divider()
        // Cadence + HR (40%)
        Box(modifier = Modifier.weight(0.4f)) {
            PowerFocusSecondary(metrics)
        }
    }
}

@Composable
private fun PowerFocusSecondary(metrics: PedalingMetrics) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${metrics.cadence}",
                color = Theme.colors.text,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rpm),
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Theme.colors.muted)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (metrics.heartRate > 0) "${metrics.heartRate}" else "--",
                color = if (metrics.heartRate > 0) Theme.colors.problem else Theme.colors.text,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.bpm),
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 18. SPRINT MODE - High-intensity metrics
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun SprintModeScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Sprint Mode", onBack)
        Divider()

        // Power + Cadence (60%)
        Box(modifier = Modifier.weight(0.6f)) {
            SprintTopView(metrics)
        }
        Divider()
        // Balance bar (40%)
        Box(modifier = Modifier.weight(0.4f)) {
            BalanceView(metrics, compact = true)
        }
    }
}

@Composable
private fun SprintTopView(metrics: PedalingMetrics) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${metrics.power}",
                color = Theme.colors.text,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.watt_symbol),
                color = Theme.colors.dim,
                fontSize = 14.sp
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(60.dp)
                .background(Theme.colors.muted)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${metrics.cadence}",
                color = Theme.colors.text,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rpm),
                color = Theme.colors.dim,
                fontSize = 14.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 19. COMPACT MULTI - All metrics in compact view
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun CompactMultiScreen(
    metrics: PedalingMetrics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        BackHeader("Compact Multi", onBack)
        Divider()

        // Power + Cadence (25%)
        Box(modifier = Modifier.weight(0.25f)) {
            CompactPowerCadence(metrics)
        }
        Divider()
        // Balance (25%)
        Box(modifier = Modifier.weight(0.25f)) {
            CompactBalance(metrics)
        }
        Divider()
        // TE (25%)
        Box(modifier = Modifier.weight(0.25f)) {
            CompactTE(metrics)
        }
        Divider()
        // PS (25%)
        Box(modifier = Modifier.weight(0.25f)) {
            CompactPS(metrics)
        }
    }
}

@Composable
private fun CompactPowerCadence(metrics: PedalingMetrics) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${metrics.power}",
                color = Theme.colors.text,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.watt_symbol),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${metrics.cadence}",
                color = Theme.colors.text,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rpm),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun CompactBalance(metrics: PedalingMetrics) {
    val left = metrics.balanceLeft.toInt()
    val right = metrics.balance.toInt()
    val status = StatusCalculator.balanceStatus(metrics.balance)
    val color = Color(StatusCalculator.statusColor(status))

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.balance),
            color = Theme.colors.dim,
            fontSize = 12.sp
        )
        Text(
            text = "$left / $right",
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompactTE(metrics: PedalingMetrics) {
    val left = metrics.torqueEffLeft.toInt()
    val right = metrics.torqueEffRight.toInt()
    val avgStatus = StatusCalculator.teStatus(metrics.torqueEffAvg)
    val color = Color(StatusCalculator.statusColor(avgStatus))

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.te),
            color = Theme.colors.dim,
            fontSize = 12.sp
        )
        Text(
            text = "$left / $right",
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompactPS(metrics: PedalingMetrics) {
    val left = metrics.pedalSmoothLeft.toInt()
    val right = metrics.pedalSmoothRight.toInt()
    val avgStatus = StatusCalculator.psStatus(metrics.pedalSmoothAvg)
    val color = Color(StatusCalculator.statusColor(avgStatus))

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.ps),
            color = Theme.colors.dim,
            fontSize = 12.sp
        )
        Text(
            text = "$left / $right",
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

