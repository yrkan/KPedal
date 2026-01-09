package io.github.kpedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import io.github.kpedal.R
import io.github.kpedal.data.models.Achievement
import io.github.kpedal.data.models.DashboardData
import io.github.kpedal.data.models.UnlockedAchievement
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.ui.theme.Theme
import io.github.kpedal.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Home Screen - Dashboard for cyclists
 *
 * Design principles for Karoo 3 (480×800px):
 * - Single column layout for readability
 * - Large touch targets for gloved fingers
 * - Rich data display - all key metrics visible
 * - Clear visual hierarchy with sections
 * - Scrollable for all content
 */
@Composable
fun HomeScreen(
    dashboardData: DashboardData,
    unlockedAchievements: List<UnlockedAchievement>,
    onNavigate: (String) -> Unit
) {
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
                .padding(horizontal = 12.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Theme.colors.optimal, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KPedal",
                    color = Theme.colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "⚙",
                color = Theme.colors.dim,
                fontSize = 18.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onNavigate("settings") }
                    .padding(8.dp)
            )
        }

        // Primary Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LIVE - Hero button
            HeroButton(
                title = stringResource(R.string.live),
                subtitle = stringResource(R.string.current_ride),
                icon = "▶",
                onClick = { onNavigate("live") }
            )

            // DRILLS - Secondary action
            ActionButton(
                title = stringResource(R.string.drills),
                subtitle = stringResource(R.string.practice),
                highlighted = true,
                onClick = { onNavigate("drills") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Section
        if (dashboardData.hasData) {
            SectionHeader(stringResource(R.string.section_progress))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Theme.colors.surface)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${dashboardData.totalRides}",
                    label = stringResource(R.string.rides)
                )
                VerticalDivider()
                StatItem(
                    value = formatBalance(dashboardData.avgBalance),
                    label = stringResource(R.string.avg_bal),
                    valueColor = getBalanceColor(dashboardData.avgBalance.toInt())
                )
                VerticalDivider()
                StatItem(
                    value = "${dashboardData.currentStreak}d",
                    label = stringResource(R.string.streak),
                    valueColor = if (dashboardData.currentStreak >= 3) Theme.colors.optimal else Theme.colors.text,
                    suffix = if (dashboardData.currentStreak >= 3) " 🔥" else ""
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Last Ride Section (detailed)
        dashboardData.lastRide?.let { ride ->
            SectionHeader(stringResource(R.string.last_ride))
            LastRideCard(ride = ride, onClick = { onNavigate("history") })
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Data Section
        SectionHeader(stringResource(R.string.section_data))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            MenuRow(
                title = stringResource(R.string.history),
                value = "${dashboardData.totalRides} ${stringResource(R.string.rides).lowercase(Locale.getDefault())}",
                onClick = { onNavigate("history") }
            )
            MenuRow(
                title = stringResource(R.string.analytics),
                value = stringResource(R.string.trends),
                onClick = { onNavigate("analytics") }
            )
            MenuRow(
                title = stringResource(R.string.achievements),
                value = "${unlockedAchievements.size}/${Achievement.all.size}",
                onClick = { onNavigate("achievements") }
            )
            MenuRow(
                title = stringResource(R.string.challenges),
                value = stringResource(R.string.weekly_goal),
                onClick = { onNavigate("challenges") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Karoo Section
        SectionHeader(stringResource(R.string.section_karoo))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            MenuRow(
                title = stringResource(R.string.data_fields),
                value = stringResource(R.string.layouts_count),
                onClick = { onNavigate("layouts") }
            )
            MenuRow(
                title = stringResource(R.string.pedal_status),
                value = stringResource(R.string.connection),
                onClick = { onNavigate("pedal-info") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Help
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            MenuRow(
                title = stringResource(R.string.help),
                value = stringResource(R.string.quick_guide),
                onClick = { onNavigate("help") }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HeroButton(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Theme.colors.surface)
            .clickable { onClick() }
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = icon,
                color = Theme.colors.optimal,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title.uppercase(),
                color = Theme.colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = subtitle,
                color = Theme.colors.dim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (highlighted) Theme.colors.optimal.copy(alpha = 0.12f)
                else Theme.colors.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Theme.colors.dim,
                fontSize = 11.sp
            )
        }
        Text(
            text = "›",
            color = Theme.colors.dim,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = Theme.colors.dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color = Theme.colors.text,
    suffix: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (suffix.isNotEmpty()) {
                Text(
                    text = suffix,
                    fontSize = 12.sp
                )
            }
        }
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Theme.colors.divider)
    )
}

@Composable
private fun LastRideCard(
    ride: RideEntity,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = remember { LocaleHelper.getCurrentLocale(context) }
    val dateFormat = remember(locale) { SimpleDateFormat("MMM d, HH:mm", locale) }
    val dateStr = dateFormat.format(Date(ride.timestamp))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Theme.colors.surface)
            .clickable { onClick() }
    ) {
        // Header: Date + Duration + Distance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateStr,
                color = Theme.colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = ride.durationFormatted,
                    color = Theme.colors.dim,
                    fontSize = 12.sp
                )
                if (ride.distanceKm > 0) {
                    Text(
                        text = String.format(locale, "%.1f km", ride.distanceKm),
                        color = Theme.colors.dim,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Score hero (if available)
        if (ride.score > 0) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${ride.score}",
                    color = getScoreColor(ride.score),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = stringResource(R.string.score),
                        color = Theme.colors.dim,
                        fontSize = 10.sp
                    )
                    Text(
                        text = getScoreLabel(ride.score),
                        color = getScoreColor(ride.score),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider()

        // Zone distribution bar
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.time_in_zone).uppercase(),
                color = Theme.colors.dim,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            ZoneBar(
                optimal = ride.zoneOptimal,
                attention = ride.zoneAttention,
                problem = ride.zoneProblem
            )
        }

        HorizontalDivider()

        // Pedaling metrics grid
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Balance
            MetricRow(
                label = stringResource(R.string.balance_lower),
                value = "${ride.balanceLeft}/${ride.balanceRight}",
                color = getBalanceColor(ride.balanceRight)
            )
            // Torque Effectiveness
            MetricRow(
                label = "TE",
                value = "${ride.teLeft}/${ride.teRight}%",
                color = getTEColor((ride.teLeft + ride.teRight) / 2)
            )
            // Pedal Smoothness
            MetricRow(
                label = "PS",
                value = "${ride.psLeft}/${ride.psRight}%",
                color = getPSColor((ride.psLeft + ride.psRight) / 2)
            )
        }

        // Extended metrics (Power, HR, etc.)
        val hasExtendedMetrics = ride.powerAvg > 0 || ride.heartRateAvg > 0 || ride.cadenceAvg > 0
        if (hasExtendedMetrics) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (ride.powerAvg > 0) {
                    MiniStatItem(
                        value = "${ride.powerAvg}",
                        unit = "W",
                        label = if (ride.normalizedPower > 0) "NP: ${ride.normalizedPower}" else "avg"
                    )
                }
                if (ride.heartRateAvg > 0) {
                    MiniStatItem(
                        value = "${ride.heartRateAvg}",
                        unit = "bpm",
                        label = "max: ${ride.heartRateMax}"
                    )
                }
                if (ride.cadenceAvg > 0) {
                    MiniStatItem(
                        value = "${ride.cadenceAvg}",
                        unit = "rpm",
                        label = "avg"
                    )
                }
            }
        }

        // Elevation (if available)
        if (ride.elevationGain > 0) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "↑ ${ride.elevationGain}m",
                    color = Theme.colors.dim,
                    fontSize = 11.sp
                )
                if (ride.elevationLoss > 0) {
                    Text(
                        text = "↓ ${ride.elevationLoss}m",
                        color = Theme.colors.dim,
                        fontSize = 11.sp
                    )
                }
                if (ride.energyKj > 0) {
                    Text(
                        text = "${ride.energyKj} kJ",
                        color = Theme.colors.dim,
                        fontSize = 11.sp
                    )
                }
            }
        }

        HorizontalDivider()

        // View details link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = stringResource(R.string.view_details) + " ›",
                color = Theme.colors.dim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ZoneBar(
    optimal: Int,
    attention: Int,
    problem: Int
) {
    val total = (optimal + attention + problem).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        if (optimal > 0) {
            Box(
                modifier = Modifier
                    .weight(optimal.toFloat() / total)
                    .fillMaxHeight()
                    .background(Theme.colors.optimal),
                contentAlignment = Alignment.Center
            ) {
                if (optimal >= 15) {
                    Text(
                        text = "$optimal%",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (attention > 0) {
            Box(
                modifier = Modifier
                    .weight(attention.toFloat() / total)
                    .fillMaxHeight()
                    .background(Theme.colors.attention),
                contentAlignment = Alignment.Center
            ) {
                if (attention >= 15) {
                    Text(
                        text = "$attention%",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (problem > 0) {
            Box(
                modifier = Modifier
                    .weight(problem.toFloat() / total)
                    .fillMaxHeight()
                    .background(Theme.colors.problem),
                contentAlignment = Alignment.Center
            ) {
                if (problem >= 15) {
                    Text(
                        text = "$problem%",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatItem(
    value: String,
    unit: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                color = Theme.colors.dim,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
        Text(
            text = label,
            color = Theme.colors.muted,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun getScoreLabel(score: Int): String {
    return when {
        score >= 90 -> stringResource(R.string.score_excellent)
        score >= 80 -> stringResource(R.string.score_great)
        score >= 70 -> stringResource(R.string.score_good)
        score >= 60 -> stringResource(R.string.score_fair)
        else -> stringResource(R.string.score_needs_work)
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 12.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, shape = CircleShape)
            )
            Text(
                text = value,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Theme.colors.divider)
    )
}

@Composable
private fun MenuRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Theme.colors.text,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = Theme.colors.dim,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "›",
                color = Theme.colors.dim,
                fontSize = 16.sp
            )
        }
    }
}

// Color functions
@Composable
private fun getBalanceColor(balanceRight: Int): Color {
    val deviation = abs(balanceRight - 50)
    return when {
        deviation <= 2 -> Theme.colors.optimal
        deviation <= 5 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getTEColor(te: Int): Color {
    return when {
        te in 70..80 -> Theme.colors.optimal
        te in 65..85 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getPSColor(ps: Int): Color {
    return when {
        ps >= 20 -> Theme.colors.optimal
        ps >= 15 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getZoneColor(zoneOptimal: Int): Color {
    return when {
        zoneOptimal >= 70 -> Theme.colors.optimal
        zoneOptimal >= 40 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getScoreColor(score: Int): Color {
    return when {
        score >= 80 -> Theme.colors.optimal
        score >= 60 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

// Formatting functions
private fun formatBalance(balance: Float): String {
    val left = (100 - balance).toInt()
    val right = balance.toInt()
    return "$left/$right"
}
