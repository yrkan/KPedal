package io.github.kpedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.data.models.Achievement
import io.github.kpedal.data.models.UnlockedAchievement
import io.github.kpedal.ui.theme.Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compact achievements screen with grouped list design.
 */
@Composable
fun AchievementsScreen(
    unlockedAchievements: List<UnlockedAchievement>,
    onBack: () -> Unit
) {
    val unlockedIds = unlockedAchievements.map { it.achievement.id }.toSet()

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
                text = "Achievements",
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${unlockedAchievements.size}/${Achievement.all.size}",
                color = Theme.colors.optimal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Theme.colors.muted)
            )
            val progress = if (Achievement.all.isNotEmpty()) {
                unlockedAchievements.size.toFloat() / Achievement.all.size
            } else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Theme.colors.optimal)
            )
        }

        Divider()

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Rides category
            AchievementCategory(
                title = "RIDES",
                achievements = listOf(
                    Achievement.FirstRide,
                    Achievement.TenRides,
                    Achievement.FiftyRides,
                    Achievement.HundredRides
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds,
                color = Theme.colors.optimal
            )

            // Balance category
            AchievementCategory(
                title = "BALANCE",
                achievements = listOf(
                    Achievement.PerfectBalance1m,
                    Achievement.PerfectBalance5m,
                    Achievement.PerfectBalance10m
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds,
                color = Theme.colors.attention
            )

            // Efficiency category
            AchievementCategory(
                title = "EFFICIENCY",
                achievements = listOf(
                    Achievement.EfficiencyMaster,
                    Achievement.SmoothOperator
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds,
                color = Theme.colors.optimal
            )

            // Streak category
            AchievementCategory(
                title = "STREAKS",
                achievements = listOf(
                    Achievement.ThreeDayStreak,
                    Achievement.SevenDayStreak,
                    Achievement.FourteenDayStreak,
                    Achievement.ThirtyDayStreak
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds,
                color = Theme.colors.problem
            )

            // Drills category
            AchievementCategory(
                title = "DRILLS",
                achievements = listOf(
                    Achievement.FirstDrill,
                    Achievement.TenDrills,
                    Achievement.PerfectDrill
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds,
                color = Theme.colors.attention
            )

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
private fun AchievementCategory(
    title: String,
    achievements: List<Achievement>,
    unlockedAchievements: List<UnlockedAchievement>,
    unlockedIds: Set<String>,
    color: Color
) {
    val unlockedCount = achievements.count { it.id in unlockedIds }

    Column {
        // Category header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Theme.colors.dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$unlockedCount/${achievements.size}",
                color = if (unlockedCount == achievements.size) color else Theme.colors.dim,
                fontSize = 10.sp
            )
        }

        // Achievement items
        achievements.forEach { achievement ->
            val unlocked = unlockedAchievements.find { it.achievement.id == achievement.id }
            val isUnlocked = unlocked != null

            AchievementRow(
                achievement = achievement,
                isUnlocked = isUnlocked,
                unlockedAt = unlocked?.unlockedAt,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    isUnlocked: Boolean,
    unlockedAt: Long?,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (isUnlocked) color else Theme.colors.muted,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "✓",
                    color = Theme.colors.background,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.name,
                color = if (isUnlocked) Theme.colors.text else Theme.colors.dim,
                fontSize = 12.sp,
                fontWeight = if (isUnlocked) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = achievement.description,
                color = Theme.colors.muted,
                fontSize = 10.sp
            )
        }

        // Unlock date
        if (isUnlocked && unlockedAt != null) {
            Text(
                text = formatDate(unlockedAt),
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
