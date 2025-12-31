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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.data.models.Achievement
import io.github.kpedal.data.models.UnlockedAchievement
import io.github.kpedal.ui.theme.Theme
import io.github.kpedal.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clean achievements screen with minimal design for Karoo 3.
 */
@Composable
fun AchievementsScreen(
    unlockedAchievements: List<UnlockedAchievement>,
    onBack: () -> Unit
) {
    val unlockedIds = remember(unlockedAchievements) {
        unlockedAchievements.map { it.achievement.id }.toSet()
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
                    .padding(end = 12.dp)
            )
            Text(
                text = stringResource(R.string.achievements),
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${unlockedAchievements.size}/${Achievement.all.size}",
                color = if (unlockedAchievements.size == Achievement.all.size) Theme.colors.optimal else Theme.colors.dim,
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
                    .background(Theme.colors.surface)
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
                .padding(top = 4.dp)
        ) {
            // Rides category
            AchievementCategory(
                title = stringResource(R.string.category_rides),
                achievements = listOf(
                    Achievement.FirstRide,
                    Achievement.TenRides,
                    Achievement.FiftyRides,
                    Achievement.HundredRides
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds
            )

            // Balance category
            AchievementCategory(
                title = stringResource(R.string.category_balance),
                achievements = listOf(
                    Achievement.PerfectBalance1m,
                    Achievement.PerfectBalance5m,
                    Achievement.PerfectBalance10m
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds
            )

            // Efficiency category
            AchievementCategory(
                title = stringResource(R.string.category_efficiency),
                achievements = listOf(
                    Achievement.EfficiencyMaster,
                    Achievement.SmoothOperator
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds
            )

            // Streak category
            AchievementCategory(
                title = stringResource(R.string.category_streaks),
                achievements = listOf(
                    Achievement.ThreeDayStreak,
                    Achievement.SevenDayStreak,
                    Achievement.FourteenDayStreak,
                    Achievement.ThirtyDayStreak
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds
            )

            // Drills category
            AchievementCategory(
                title = stringResource(R.string.category_drills),
                achievements = listOf(
                    Achievement.FirstDrill,
                    Achievement.TenDrills,
                    Achievement.PerfectDrill
                ),
                unlockedAchievements = unlockedAchievements,
                unlockedIds = unlockedIds
            )

            Spacer(modifier = Modifier.height(12.dp))
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
    unlockedIds: Set<String>
) {
    val unlockedCount = achievements.count { it.id in unlockedIds }
    val allUnlocked = unlockedCount == achievements.size

    Column {
        // Category header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Theme.colors.dim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$unlockedCount/${achievements.size}",
                color = if (allUnlocked) Theme.colors.optimal else Theme.colors.dim,
                fontSize = 11.sp
            )
        }

        // Achievement items
        achievements.forEach { achievement ->
            val unlocked = unlockedAchievements.find { it.achievement.id == achievement.id }
            val isUnlocked = unlocked != null

            AchievementRow(
                achievement = achievement,
                isUnlocked = isUnlocked,
                unlockedAt = unlocked?.unlockedAt
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    isUnlocked: Boolean,
    unlockedAt: Long?
) {
    val context = LocalContext.current
    val locale = remember { LocaleHelper.getCurrentLocale(context) }
    val dateFormat = remember(locale) { SimpleDateFormat("MMM d", locale) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) Theme.colors.optimal else Theme.colors.surface)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Name and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(achievement.nameRes),
                color = if (isUnlocked) Theme.colors.text else Theme.colors.dim,
                fontSize = 13.sp,
                fontWeight = if (isUnlocked) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = stringResource(achievement.descriptionRes),
                color = if (isUnlocked) Theme.colors.dim else Theme.colors.muted,
                fontSize = 11.sp
            )
        }

        // Unlock date
        if (isUnlocked && unlockedAt != null) {
            val dateStr = dateFormat.format(Date(unlockedAt))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            Text(
                text = dateStr,
                color = Theme.colors.optimal,
                fontSize = 11.sp
            )
        }
    }
}
