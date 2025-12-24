package io.github.kpedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import io.github.kpedal.data.models.Achievement
import io.github.kpedal.data.models.DashboardData
import io.github.kpedal.data.models.UnlockedAchievement
import io.github.kpedal.ui.theme.Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Home Screen - Clean navigation for Karoo 3
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
                .padding(top = 20.dp, bottom = 12.dp),
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Settings icon
            Text(
                text = "⚙",
                color = Theme.colors.dim,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onNavigate("settings") }
                    .padding(4.dp)
            )
        }

        // Primary Actions (Live / Drills buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionCard(
                title = "Live",
                subtitle = "Current ride",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("live") }
            )
            ActionCard(
                title = "Drills",
                subtitle = "Practice",
                highlighted = true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("drills") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Stats (compact inline)
        if (dashboardData.hasData) {
            QuickStats(
                totalRides = dashboardData.totalRides,
                avgBalance = dashboardData.avgBalance,
                streak = dashboardData.currentStreak,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Last Ride (if exists)
        dashboardData.lastRide?.let { ride ->
            LastRideRow(
                date = formatDate(ride.timestamp),
                balanceLeft = ride.balanceLeft,
                balanceRight = ride.balanceRight,
                zoneOptimal = ride.zoneOptimal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                onClick = { onNavigate("history") }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Menu sections
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            // Data section
            SectionLabel("DATA")
            Spacer(modifier = Modifier.height(4.dp))

            MenuRow("History", "Saved rides") { onNavigate("history") }
            MenuRow("Analytics", "Trends") { onNavigate("analytics") }
            MenuRow(
                "Achievements",
                "${unlockedAchievements.size}/${Achievement.all.size}"
            ) { onNavigate("achievements") }
            MenuRow("Challenges", "Weekly goal") { onNavigate("challenges") }

            Spacer(modifier = Modifier.height(14.dp))

            // Karoo section
            SectionLabel("KAROO")
            Spacer(modifier = Modifier.height(4.dp))

            MenuRow("Data Fields", "6 layouts") { onNavigate("layouts") }
            MenuRow("Pedal Status", "Connection") { onNavigate("pedal-info") }

            Spacer(modifier = Modifier.height(14.dp))

            // More section
            SectionLabel("MORE")
            Spacer(modifier = Modifier.height(4.dp))

            MenuRow("Help", "Quick guide") { onNavigate("help") }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun QuickStats(
    totalRides: Int,
    avgBalance: Float,
    streak: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(value = "$totalRides", label = "rides")
        Divider()
        StatItem(
            value = formatBalance(avgBalance),
            label = "avg bal",
            valueColor = getBalanceColor(avgBalance.toInt())
        )
        Divider()
        StatItem(
            value = "${streak}d",
            label = "streak",
            valueColor = if (streak >= 3) Theme.colors.optimal else Theme.colors.text
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color = Theme.colors.text
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
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
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Theme.colors.divider)
    )
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (highlighted) Theme.colors.optimal.copy(alpha = 0.12f)
                else Theme.colors.surface
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = Theme.colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            color = Theme.colors.dim,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun LastRideRow(
    date: String,
    balanceLeft: Int,
    balanceRight: Int,
    zoneOptimal: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Last ride",
                color = Theme.colors.dim,
                fontSize = 9.sp
            )
            Text(
                text = date,
                color = Theme.colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$balanceLeft/$balanceRight",
                    color = getBalanceColor(balanceRight),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(text = "L/R", color = Theme.colors.dim, fontSize = 8.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$zoneOptimal%",
                    color = getZoneColor(zoneOptimal),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(text = "optimal", color = Theme.colors.dim, fontSize = 8.sp)
            }
        }
    }
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
private fun MenuRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Theme.colors.text,
            fontSize = 13.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subtitle,
                color = Theme.colors.dim,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "›",
                color = Theme.colors.dim,
                fontSize = 14.sp
            )
        }
    }
}

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
private fun getZoneColor(zoneOptimal: Int): Color {
    return when {
        zoneOptimal >= 70 -> Theme.colors.optimal
        zoneOptimal >= 40 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

private fun formatBalance(balance: Float): String {
    val left = (100 - balance).toInt()
    val right = balance.toInt()
    return "$left/$right"
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
