package io.github.kpedal.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.data.models.ChallengeProgress
import io.github.kpedal.data.models.WeeklyChallenges
import io.github.kpedal.ui.theme.Theme
import java.util.Calendar

/**
 * Weekly Challenges screen.
 */
@Composable
fun ChallengesScreen(
    progress: ChallengeProgress?,
    onBack: () -> Unit
) {
    val currentChallenge = WeeklyChallenges.getCurrentChallenge()
    val nextChallenge = WeeklyChallenges.getNextChallenge()

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Theme.colors.dim,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() }
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.weekly_challenge),
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Divider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Days remaining
            val daysLeft = getDaysLeftInWeek()
            Text(
                text = stringResource(R.string.days_left_this_week, daysLeft),
                color = Theme.colors.dim,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Current challenge card
            CurrentChallengeCard(
                challenge = currentChallenge,
                progress = progress
            )

            Spacer(modifier = Modifier.height(20.dp))

            // All challenges list
            SectionLabel(stringResource(R.string.all_challenges))
            Spacer(modifier = Modifier.height(8.dp))

            WeeklyChallenges.challenges.forEach { challenge ->
                val isCurrent = challenge.id == currentChallenge.id
                ChallengeRow(
                    challenge = challenge,
                    isCurrent = isCurrent,
                    isCompleted = progress?.isComplete == true && isCurrent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Next week preview
            SectionLabel(stringResource(R.string.next_week))
            Spacer(modifier = Modifier.height(8.dp))

            NextChallengePreview(challenge = nextChallenge)

            Spacer(modifier = Modifier.height(16.dp))
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Theme.colors.dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun CurrentChallengeCard(
    challenge: WeeklyChallenges.Challenge,
    progress: ChallengeProgress?
) {
    val currentProgress = progress?.currentProgress ?: 0
    val progressPercent = progress?.progressPercent ?: 0
    val isComplete = progress?.isComplete == true
    val completedText = stringResource(R.string.completed_upper)
    val thisWeekText = stringResource(R.string.this_week)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isComplete) Theme.colors.optimal.copy(alpha = 0.15f)
                else Theme.colors.surface
            )
            .padding(16.dp)
    ) {
        // Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isComplete) completedText else thisWeekText,
                color = if (isComplete) Theme.colors.optimal else Theme.colors.dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (isComplete) {
                Text(
                    text = "✓",
                    color = Theme.colors.optimal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Challenge name
        Text(
            text = challenge.name,
            color = Theme.colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Description
        Text(
            text = challenge.description,
            color = Theme.colors.dim,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Theme.colors.muted)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressPercent / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isComplete) Theme.colors.optimal
                        else Theme.colors.attention
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$currentProgress / ${challenge.target} ${challenge.unit}",
                color = Theme.colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$progressPercent%",
                color = if (isComplete) Theme.colors.optimal else Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ChallengeRow(
    challenge: WeeklyChallenges.Challenge,
    isCurrent: Boolean,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        isCompleted -> Theme.colors.optimal
                        isCurrent -> Theme.colors.attention
                        else -> Theme.colors.muted
                    }
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = challenge.name,
                color = if (isCurrent) Theme.colors.text else Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = challenge.description,
                color = Theme.colors.muted,
                fontSize = 10.sp
            )
        }

        if (isCurrent) {
            Text(
                text = stringResource(R.string.active),
                color = Theme.colors.attention,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NextChallengePreview(
    challenge: WeeklyChallenges.Challenge
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = challenge.name,
                color = Theme.colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = challenge.description,
                color = Theme.colors.dim,
                fontSize = 10.sp
            )
        }
        Text(
            text = "→",
            color = Theme.colors.dim,
            fontSize = 14.sp
        )
    }
}

private fun getDaysLeftInWeek(): Int {
    val cal = Calendar.getInstance()
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Sunday = 1, Saturday = 7
    // Days until Sunday (end of week)
    return if (dayOfWeek == Calendar.SUNDAY) 0 else 8 - dayOfWeek
}
