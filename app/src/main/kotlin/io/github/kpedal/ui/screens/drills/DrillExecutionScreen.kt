package io.github.kpedal.ui.screens.drills

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.drill.model.*
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.ui.theme.Theme

@Composable
fun DrillExecutionScreen(
    state: DrillExecutionState,
    metrics: PedalingMetrics,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val isPaused = state.status == DrillExecutionStatus.PAUSED
    val isCountdown = state.status == DrillExecutionStatus.COUNTDOWN
    val currentPhase = state.currentPhase

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        if (isCountdown) {
            // Countdown
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.countdownSeconds}",
                    color = Theme.colors.optimal,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: time + cadence + phase
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.formatTimeRemaining(),
                        color = Theme.colors.text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Cadence display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${metrics.cadence}",
                            color = if (metrics.cadence >= 100) Theme.colors.optimal else Theme.colors.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.rpm),
                            color = Theme.colors.dim,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = "${state.currentPhaseIndex + 1}/${state.drill.phases.size}",
                        color = Theme.colors.dim,
                        fontSize = 13.sp
                    )
                }

                // Progress
                ProgressBar(
                    progress = state.progressPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                // Phase info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current phase
                    Column {
                        Text(
                            text = currentPhase?.let { it.nameOverride ?: stringResource(it.nameRes) } ?: "",
                            color = Theme.colors.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = state.formatPhaseTimeRemaining(),
                            color = Theme.colors.dim,
                            fontSize = 11.sp
                        )
                    }

                    // Next phase preview
                    state.nextPhase?.let { next ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.next),
                                color = Theme.colors.dim,
                                fontSize = 10.sp
                            )
                            Text(
                                text = next.nameOverride ?: stringResource(next.nameRes),
                                color = Theme.colors.dim,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Divider()

                // Main content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.hasTarget && currentPhase?.target != null) {
                        TargetContent(
                            state = state,
                            target = currentPhase.target,
                            metrics = metrics
                        )
                    } else {
                        RecoveryContent(phase = currentPhase, defaultText = stringResource(R.string.spin_easy))
                    }
                }

                Divider()

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Theme.colors.muted)
                            .clickable { onStop() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.stop),
                            color = Theme.colors.problem,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Theme.colors.muted)
                            .clickable { onPause() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.pause),
                            color = Theme.colors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Pause overlay
            if (isPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.colors.background.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.paused),
                            color = Theme.colors.dim,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.formatTimeRemaining(),
                            color = Theme.colors.text,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Resume button - primary action
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Theme.colors.optimal)
                                .clickable { onResume() }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.resume),
                                color = Theme.colors.background,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stop button - secondary action
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Theme.colors.muted)
                                .clickable { onStop() }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.stop),
                                color = Theme.colors.problem,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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
private fun TargetContent(
    state: DrillExecutionState,
    target: DrillTarget,
    metrics: PedalingMetrics
) {
    val currentValue = when (target.metric) {
        DrillMetric.BALANCE -> metrics.balance
        DrillMetric.TORQUE_EFFECTIVENESS -> metrics.torqueEffAvg
        DrillMetric.PEDAL_SMOOTHNESS -> metrics.pedalSmoothAvg
        DrillMetric.COMBINED -> 0f
    }

    val isInTarget = state.isInTarget
    val statusColor = if (isInTarget) Theme.colors.optimal else Theme.colors.problem

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Value
        if (target.metric == DrillMetric.BALANCE) {
            val left = (100 - currentValue).toInt()
            val right = currentValue.toInt()
            Text(
                text = "$left / $right",
                color = statusColor,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "${currentValue.toInt()}%",
                color = statusColor,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = target.description(),
            color = Theme.colors.dim,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatTime(state.targetHoldMs),
            color = if (isInTarget) Theme.colors.optimal else Theme.colors.dim,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.in_zone),
            color = Theme.colors.dim,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun RecoveryContent(phase: DrillPhase?, defaultText: String) {
    val instruction = when {
        phase?.instructionOverride != null -> phase.instructionOverride
        phase != null && phase.instructionRes != 0 -> stringResource(phase.instructionRes)
        else -> defaultText
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = instruction,
            color = Theme.colors.dim,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "progress"
    )

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Theme.colors.muted)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(Theme.colors.optimal)
        )
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "$minutes:${secs.toString().padStart(2, '0')}"
    } else {
        "${secs}s"
    }
}
