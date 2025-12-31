package io.github.kpedal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import io.github.kpedal.R
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.data.models.DashboardData
import io.github.kpedal.ui.theme.Theme
import io.github.kpedal.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Dashboard section for HomeScreen showing quick stats.
 */
@Composable
fun DashboardSection(
    data: DashboardData,
    modifier: Modifier = Modifier
) {
    if (!data.hasData) return

    Column(modifier = modifier) {
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = stringResource(R.string.rides),
                value = data.totalRides.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.avg_balance_label),
                value = formatBalance(data.avgBalance),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.streak),
                value = stringResource(R.string.streak_days, data.currentStreak),
                valueColor = if (data.currentStreak >= 3) Theme.colors.optimal else Theme.colors.text,
                modifier = Modifier.weight(1f)
            )
        }

        // Last ride info
        data.lastRide?.let { ride ->
            Spacer(modifier = Modifier.height(8.dp))
            LastRideCard(ride = ride)
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = Theme.colors.text
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = valueColor,
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
private fun LastRideCard(ride: RideEntity) {
    val context = LocalContext.current
    val locale = remember { LocaleHelper.getCurrentLocale(context) }
    val dateFormat = remember(locale) { SimpleDateFormat("MMM d, HH:mm", locale) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.last_ride),
                color = Theme.colors.dim,
                fontSize = 9.sp
            )
            Text(
                text = dateFormat.format(Date(ride.timestamp)),
                color = Theme.colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Balance
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.lr_balance_format, ride.balanceLeft, ride.balanceRight),
                    color = getBalanceColor(ride.balanceRight),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.balance_lower),
                    color = Theme.colors.dim,
                    fontSize = 8.sp
                )
            }

            // Zone optimal
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${ride.zoneOptimal}%",
                    color = getZoneColor(ride.zoneOptimal),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.optimal),
                    color = Theme.colors.dim,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun getBalanceColor(balanceRight: Int): androidx.compose.ui.graphics.Color {
    val deviation = abs(balanceRight - 50)
    return when {
        deviation <= 2 -> Theme.colors.optimal
        deviation <= 5 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

@Composable
private fun getZoneColor(zoneOptimal: Int): androidx.compose.ui.graphics.Color {
    return when {
        zoneOptimal >= 70 -> Theme.colors.optimal
        zoneOptimal >= 40 -> Theme.colors.attention
        else -> Theme.colors.problem
    }
}

private fun formatBalance(balance: Float): String {
    val left = (100 - balance).toInt()
    val right = balance.toInt()
    return "L$left/R$right"
}
