package io.github.kpedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.data.models.RideFilter
import io.github.kpedal.ui.theme.Theme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * History screen showing list of saved rides with filtering.
 */
@Composable
fun HistoryScreen(
    rides: List<RideEntity>,
    onBack: () -> Unit,
    onRideClick: (Long) -> Unit,
    onDeleteRide: (Long) -> Unit
) {
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }
    var filter by remember { mutableStateOf(RideFilter()) }

    // Apply filter to rides
    val filteredRides = remember(rides, filter) {
        applyFilter(rides, filter)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onBack() }
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "Ride History",
                        color = Theme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (filter.isActive) "${filteredRides.size}/${rides.size}" else "${rides.size} rides",
                    color = if (filter.isActive) Theme.colors.optimal else Theme.colors.dim,
                    fontSize = 12.sp
                )
            }

            // Filter chips
            FilterChips(
                filter = filter,
                onFilterChange = { filter = it }
            )

            Divider()

            if (filteredRides.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (filter.isActive) "No matching rides" else "No rides saved",
                            color = Theme.colors.dim,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (filter.isActive) "Try adjusting the filter" else "Rides are saved automatically",
                            color = Theme.colors.muted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredRides, key = { it.id }) { ride ->
                        RideListItem(
                            ride = ride,
                            onClick = { onRideClick(ride.id) },
                            onDeleteClick = { deleteConfirmId = ride.id }
                        )
                    }
                }
            }
        }

        // Delete confirmation overlay
        deleteConfirmId?.let { id ->
            DeleteConfirmDialog(
                onConfirm = {
                    onDeleteRide(id)
                    deleteConfirmId = null
                },
                onDismiss = { deleteConfirmId = null }
            )
        }
    }
}

@Composable
private fun RideListItem(
    ride: RideEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(ride.timestamp))

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateStr,
                        color = Theme.colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (ride.savedManually) {
                        Text(
                            text = "manual",
                            color = Theme.colors.attention,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = ride.durationFormatted,
                        color = Theme.colors.dim,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "L${ride.balanceLeft}/R${ride.balanceRight}",
                        color = Theme.colors.dim,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${ride.zoneOptimal}% optimal",
                        color = Theme.colors.optimal,
                        fontSize = 11.sp
                    )
                }
            }

            // Delete button
            Text(
                text = "×",
                color = Theme.colors.dim,
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable { onDeleteClick() }
                    .padding(start = 8.dp)
            )
        }
        Divider()
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
                text = "Delete ride?",
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Cancel",
                    color = Theme.colors.dim,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Text(
                    text = "Delete",
                    color = Theme.colors.problem,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onConfirm() }
                )
            }
        }
    }
}

// ========== Filter UI ==========

@Composable
private fun FilterChips(
    filter: RideFilter,
    onFilterChange: (RideFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date range chips
        RideFilter.DateRange.entries.forEach { dateRange ->
            FilterChip(
                label = dateRange.label,
                isSelected = filter.dateRange == dateRange,
                onClick = { onFilterChange(filter.copy(dateRange = dateRange)) }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Balance filter chips
        RideFilter.BalanceFilter.entries.forEach { balanceFilter ->
            FilterChip(
                label = balanceFilter.label,
                isSelected = filter.balanceFilter == balanceFilter,
                onClick = { onFilterChange(filter.copy(balanceFilter = balanceFilter)) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (isSelected) Theme.colors.background else Theme.colors.text,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Theme.colors.optimal else Theme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ========== Filter Logic ==========

private fun applyFilter(rides: List<RideEntity>, filter: RideFilter): List<RideEntity> {
    var result = rides

    // Apply date filter
    filter.dateRange.daysBack?.let { days ->
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        result = result.filter { it.timestamp >= cutoff }
    }

    // Apply balance filter
    result = when (filter.balanceFilter) {
        RideFilter.BalanceFilter.OPTIMAL_ONLY -> result.filter {
            abs(it.balanceRight - 50) <= 5
        }
        RideFilter.BalanceFilter.PROBLEM_ONLY -> result.filter {
            abs(it.balanceRight - 50) > 5
        }
        RideFilter.BalanceFilter.ALL -> result
    }

    return result
}
