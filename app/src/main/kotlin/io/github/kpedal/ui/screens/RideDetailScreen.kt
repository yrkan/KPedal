package io.github.kpedal.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.data.models.RideAnalyzer
import io.github.kpedal.engine.StatusCalculator
import io.github.kpedal.ui.theme.Theme
import io.github.kpedal.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detail screen for a saved ride.
 * Clean, informative layout optimized for Karoo 3 (480×800px).
 */
@Composable
fun RideDetailScreen(
    ride: RideEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onRatingChange: (Int) -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var currentRating by remember { mutableStateOf(ride.rating) }
    val context = LocalContext.current
    val locale = remember { LocaleHelper.getCurrentLocale(context) }
    val dateFormat = remember(locale) { SimpleDateFormat("MMM d, HH:mm", locale) }
    val dateStr = dateFormat.format(Date(ride.timestamp))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    val analysis = remember(ride) { RideAnalyzer.analyze(ride) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background)
                .verticalScroll(rememberScrollState())
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateStr,
                        color = Theme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = ride.durationFormatted,
                        color = Theme.colors.dim,
                        fontSize = 11.sp
                    )
                }
            }

            // Hero Score
            HeroScore(
                score = analysis.overallScore,
                balanceScore = analysis.balanceScore,
                efficiencyScore = analysis.efficiencyScore,
                consistencyScore = analysis.consistencyScore
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Balance
            BalanceRow(ride.balanceLeft, ride.balanceRight)

            Spacer(modifier = Modifier.height(12.dp))

            // TE & PS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "TE",
                    left = ride.teLeft,
                    right = ride.teRight,
                    statusFn = { StatusCalculator.teStatus(it.toFloat()) },
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "PS",
                    left = ride.psLeft,
                    right = ride.psRight,
                    statusFn = { StatusCalculator.psStatus(it.toFloat()) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time in Zone
            ZoneBar(ride.zoneOptimal, ride.zoneAttention, ride.zoneProblem)

            Spacer(modifier = Modifier.height(12.dp))

            // Analysis
            if (analysis.strengthResIds.isNotEmpty() || analysis.improvementResIds.isNotEmpty()) {
                AnalysisSection(
                    summaryResId = analysis.summaryResId,
                    strengthResIds = analysis.strengthResIds,
                    improvementResIds = analysis.improvementResIds
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Rating
            RatingRow(
                rating = currentRating,
                suggestedRating = analysis.suggestedRating,
                onRatingChange = { newRating ->
                    currentRating = newRating
                    onRatingChange(newRating)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Delete
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.delete_ride),
                    color = Theme.colors.muted,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { showDeleteConfirm = true }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Delete confirmation
        if (showDeleteConfirm) {
            DeleteConfirmOverlay(
                onConfirm = onDelete,
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

@Composable
private fun HeroScore(
    score: Int,
    balanceScore: Int,
    efficiencyScore: Int,
    consistencyScore: Int
) {
    val scoreColor = when {
        score >= 80 -> Theme.colors.optimal
        score >= 60 -> Theme.colors.attention
        else -> Theme.colors.problem
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$score",
            color = scoreColor,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.overall).uppercase(),
            color = Theme.colors.dim,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-scores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SubScore(stringResource(R.string.balance_lower), balanceScore)
            SubScore(stringResource(R.string.efficiency), efficiencyScore)
            SubScore(stringResource(R.string.consistency), consistencyScore)
        }
    }
}

@Composable
private fun SubScore(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$score",
            color = Theme.colors.text,
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
private fun BalanceRow(left: Int, right: Int) {
    val status = StatusCalculator.balanceStatus(right.toFloat())
    val barColor = Color(StatusCalculator.statusColor(status))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "$left",
                    color = Theme.colors.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.left),
                    color = Theme.colors.dim,
                    fontSize = 10.sp
                )
            }
            Text(
                text = stringResource(R.string.balance).uppercase(),
                color = Theme.colors.dim,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$right",
                    color = Theme.colors.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.right),
                    color = Theme.colors.dim,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Balance bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Theme.colors.surface)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(left.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(barColor.copy(alpha = 0.6f))
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
                        .background(barColor.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    left: Int,
    right: Int,
    statusFn: (Int) -> StatusCalculator.Status,
    modifier: Modifier = Modifier
) {
    val leftColor = Color(StatusCalculator.statusColor(statusFn(left)))
    val rightColor = Color(StatusCalculator.statusColor(statusFn(right)))

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$left",
                    color = leftColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.left),
                    color = Theme.colors.muted,
                    fontSize = 8.sp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$right",
                    color = rightColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.right),
                    color = Theme.colors.muted,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun ZoneBar(optimal: Int, attention: Int, problem: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.time_in_zone).uppercase(),
                color = Theme.colors.dim,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ZoneLabel(optimal, Theme.colors.optimal)
                ZoneLabel(attention, Theme.colors.attention)
                ZoneLabel(problem, Theme.colors.problem)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            val total = (optimal + attention + problem).coerceAtLeast(1)
            if (optimal > 0) {
                Box(
                    modifier = Modifier
                        .weight(optimal.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.optimal)
                )
            }
            if (attention > 0) {
                Box(
                    modifier = Modifier
                        .weight(attention.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.attention)
                )
            }
            if (problem > 0) {
                Box(
                    modifier = Modifier
                        .weight(problem.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.problem)
                )
            }
            if (total <= 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Theme.colors.surface)
                )
            }
        }
    }
}

@Composable
private fun ZoneLabel(value: Int, color: Color) {
    Text(
        text = "$value%",
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun AnalysisSection(
    summaryResId: Int,
    strengthResIds: List<Int>,
    improvementResIds: List<Int>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(10.dp)
    ) {
        Text(
            text = stringResource(summaryResId),
            color = Theme.colors.text,
            fontSize = 11.sp
        )

        if (strengthResIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            strengthResIds.forEach { resId ->
                Text(
                    text = "✓ ${stringResource(resId)}",
                    color = Theme.colors.optimal,
                    fontSize = 10.sp
                )
            }
        }

        if (improvementResIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            improvementResIds.forEach { resId ->
                Text(
                    text = "→ ${stringResource(resId)}",
                    color = Theme.colors.attention,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RatingRow(
    rating: Int,
    suggestedRating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.rating),
                color = Theme.colors.dim,
                fontSize = 10.sp
            )
            if (rating == 0) {
                Text(
                    text = stringResource(R.string.suggested_stars, suggestedRating),
                    color = Theme.colors.muted,
                    fontSize = 8.sp
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { star ->
                Text(
                    text = if (star <= rating) "★" else "☆",
                    color = if (star <= rating) Theme.colors.attention else Theme.colors.muted,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onRatingChange(if (rating == star) 0 else star)
                    }
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmOverlay(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background.copy(alpha = 0.95f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.delete_ride),
                color = Theme.colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = Theme.colors.dim,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Text(
                    text = stringResource(R.string.delete),
                    color = Theme.colors.problem,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onConfirm() }
                )
            }
        }
    }
}
