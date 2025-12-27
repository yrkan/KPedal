package io.github.kpedal.ui.screens.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import io.github.kpedal.drill.model.*
import io.github.kpedal.ui.theme.Theme

/**
 * Screen showing drill details before starting.
 */
@Composable
fun DrillDetailScreen(
    drill: Drill,
    bestScore: Float?,
    completedCount: Int,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val metricColor = when (drill.metric) {
        DrillMetric.BALANCE -> Theme.colors.attention
        DrillMetric.TORQUE_EFFECTIVENESS -> Theme.colors.optimal
        DrillMetric.PEDAL_SMOOTHNESS -> Theme.colors.problem
        DrillMetric.COMBINED -> Theme.colors.text
    }

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
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBack() }
                        .padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = drill.nameOverride ?: stringResource(drill.nameRes),
                        color = Theme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = drill.durationFormatted,
                        color = Theme.colors.dim,
                        fontSize = 10.sp
                    )
                }
            }
            // Stats in header
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                bestScore?.let {
                    Text(
                        text = stringResource(R.string.best_score, it.toInt()),
                        color = Theme.colors.optimal,
                        fontSize = 11.sp
                    )
                }
                if (completedCount > 0) {
                    Text(
                        text = "${completedCount}×",
                        color = Theme.colors.dim,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Divider()

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Description
            Text(
                text = drill.descriptionOverride ?: stringResource(drill.descriptionRes),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(12.dp)
            )

            Divider()

            // Phases - clean timeline
            drill.phases.forEachIndexed { index, phase ->
                PhaseItem(
                    phase = phase,
                    isLast = index == drill.phases.lastIndex,
                    metricColor = metricColor
                )
            }
        }

        // Start button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.optimal)
                .clickable { onStart() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.start),
                color = Theme.colors.background,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
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
private fun PhaseItem(
    phase: DrillPhase,
    isLast: Boolean,
    metricColor: Color
) {
    val hasTarget = phase.target != null

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Indicator
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(if (hasTarget) 24.dp else 16.dp)
                        .background(if (hasTarget) metricColor else Theme.colors.dim.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = phase.nameOverride ?: stringResource(phase.nameRes),
                        color = Theme.colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    phase.target?.let { target ->
                        Text(
                            text = target.description(),
                            color = metricColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                text = "${phase.durationMs / 1000}s",
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
        }

        if (!isLast) {
            Divider()
        }
    }
}
