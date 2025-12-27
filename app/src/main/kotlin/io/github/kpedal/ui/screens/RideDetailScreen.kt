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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.data.models.RideAnalyzer
import io.github.kpedal.engine.StatusCalculator
import io.github.kpedal.ui.theme.Theme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detail screen for a saved ride - mirrors SummaryScreen layout.
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
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(ride.timestamp))
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
                    .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
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
                    Column {
                        Text(
                            text = dateStr,
                            color = Theme.colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ride.durationFormatted,
                            color = Theme.colors.dim,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.delete),
                    color = Theme.colors.problem,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { showDeleteConfirm = true }
                )
            }

            // Rating section
            RatingSection(
                rating = currentRating,
                suggestedRating = analysis.suggestedRating,
                onRatingChange = { newRating ->
                    currentRating = newRating
                    onRatingChange(newRating)
                }
            )

            Divider()

            // Score overview
            ScoreOverview(
                overallScore = analysis.overallScore,
                balanceScore = analysis.balanceScore,
                efficiencyScore = analysis.efficiencyScore,
                consistencyScore = analysis.consistencyScore
            )

            Divider()

            // BALANCE section
            BalanceSection(ride.balanceLeft, ride.balanceRight)

            Divider()

            // TE & PS row
            Row(modifier = Modifier.height(100.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    MetricSection("TE", ride.teLeft, ride.teRight) {
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
                    MetricSection("PS", ride.psLeft, ride.psRight) {
                        StatusCalculator.psStatus(it.toFloat())
                    }
                }
            }

            Divider()

            // TIME IN ZONE section
            TimeInZoneSection(ride.zoneOptimal, ride.zoneAttention, ride.zoneProblem)

            Divider()

            // Analysis insights
            AnalysisSection(
                strengths = analysis.strengths,
                improvements = analysis.improvements,
                summary = analysis.summary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Delete confirmation overlay
        if (showDeleteConfirm) {
            DeleteConfirmDialog(
                onConfirm = onDelete,
                onDismiss = { showDeleteConfirm = false }
            )
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
private fun RatingSection(
    rating: Int,
    suggestedRating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.rating),
                color = Theme.colors.dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            if (rating == 0) {
                Text(
                    text = stringResource(R.string.suggested_stars, suggestedRating),
                    color = Theme.colors.muted,
                    fontSize = 9.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { star ->
                Text(
                    text = if (star <= rating) "★" else "☆",
                    color = if (star <= rating) Theme.colors.attention else Theme.colors.muted,
                    fontSize = 22.sp,
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
private fun ScoreOverview(
    overallScore: Int,
    balanceScore: Int,
    efficiencyScore: Int,
    consistencyScore: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ScoreItem(stringResource(R.string.overall), overallScore, true)
        ScoreItem(stringResource(R.string.balance_lower), balanceScore)
        ScoreItem(stringResource(R.string.efficiency), efficiencyScore)
        ScoreItem(stringResource(R.string.consistency), consistencyScore)
    }
}

@Composable
private fun ScoreItem(label: String, score: Int, isPrimary: Boolean = false) {
    val scoreColor = when {
        score >= 80 -> Theme.colors.optimal
        score >= 60 -> Theme.colors.attention
        else -> Theme.colors.problem
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$score",
            color = if (isPrimary) scoreColor else Theme.colors.text,
            fontSize = if (isPrimary) 24.sp else 16.sp,
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
private fun AnalysisSection(
    strengths: List<String>,
    improvements: List<String>,
    summary: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.analysis),
            color = Theme.colors.dim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = summary,
            color = Theme.colors.text,
            fontSize = 12.sp
        )

        if (strengths.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.strengths),
                color = Theme.colors.optimal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            strengths.forEach { strength ->
                Text(
                    text = "• $strength",
                    color = Theme.colors.text,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (improvements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.areas_to_improve),
                color = Theme.colors.attention,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            improvements.forEach { improvement ->
                Text(
                    text = "• $improvement",
                    color = Theme.colors.text,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BalanceSection(balanceLeft: Int, balanceRight: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.balance),
            color = Theme.colors.dim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$balanceLeft",
                color = Theme.colors.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$balanceRight",
                color = Theme.colors.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
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
                        .weight(balanceLeft.toFloat().coerceAtLeast(1f))
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
                        .weight(balanceRight.toFloat().coerceAtLeast(1f))
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
            Text(text = stringResource(R.string.left), color = Theme.colors.dim, fontSize = 12.sp)
            Text(text = stringResource(R.string.right), color = Theme.colors.dim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MetricSection(
    label: String,
    left: Int,
    right: Int,
    statusFn: (Int) -> StatusCalculator.Status
) {
    val leftStatus = statusFn(left)
    val rightStatus = statusFn(right)

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.left), color = Theme.colors.dim, fontSize = 10.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$right",
                    color = Color(StatusCalculator.statusColor(rightStatus)),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.right), color = Theme.colors.dim, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TimeInZoneSection(zoneOptimal: Int, zoneAttention: Int, zoneProblem: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.time_in_zone),
            color = Theme.colors.dim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$zoneOptimal%",
                    color = Theme.colors.optimal,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.optimal), color = Theme.colors.dim, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$zoneAttention%",
                    color = Theme.colors.attention,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.attention), color = Theme.colors.dim, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$zoneProblem%",
                    color = Theme.colors.problem,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.problem), color = Theme.colors.dim, fontSize = 9.sp)
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
            val total = (zoneOptimal + zoneAttention + zoneProblem).coerceAtLeast(1)

            if (zoneOptimal > 0) {
                Box(
                    modifier = Modifier
                        .weight(zoneOptimal.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.optimal)
                )
            }
            if (zoneAttention > 0) {
                Box(
                    modifier = Modifier
                        .weight(zoneAttention.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.attention)
                )
            }
            if (zoneProblem > 0) {
                Box(
                    modifier = Modifier
                        .weight(zoneProblem.toFloat() / total)
                        .fillMaxHeight()
                        .background(Theme.colors.problem)
                )
            }
            if (total <= 1 && zoneOptimal == 0) {
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

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background.copy(alpha = 0.9f))
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = Theme.colors.dim,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Text(
                    text = stringResource(R.string.delete),
                    color = Theme.colors.problem,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onConfirm() }
                )
            }
        }
    }
}
