package io.github.kpedal.ui.screens.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.drill.model.DrillResult
import io.github.kpedal.ui.theme.Theme

@Composable
fun DrillResultScreen(
    result: DrillResult,
    previousBestScore: Float?,
    onDone: () -> Unit,
    onRetry: () -> Unit
) {
    val isNewBest = previousBestScore != null && result.completed && result.score > previousBestScore

    val scoreColor = when {
        result.score >= 75 -> Theme.colors.optimal
        result.score >= 50 -> Theme.colors.attention
        else -> Theme.colors.problem
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
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(if (result.completed) R.string.complete else R.string.stopped),
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Divider()

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Score
                Text(
                    text = "${result.score.toInt()}%",
                    color = scoreColor,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = result.drillName,
                    color = Theme.colors.dim,
                    fontSize = 12.sp
                )

                if (isNewBest) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.new_best),
                        color = Theme.colors.optimal,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = result.durationFormatted,
                            color = Theme.colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.duration),
                            color = Theme.colors.dim,
                            fontSize = 10.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.timeInTargetPercent.toInt()}%",
                            color = Theme.colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.in_zone),
                            color = Theme.colors.dim,
                            fontSize = 10.sp
                        )
                    }
                }
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
                    .clickable { onDone() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.done),
                    color = Theme.colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Theme.colors.optimal)
                    .clickable { onRetry() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    color = Theme.colors.background,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
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
