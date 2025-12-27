package io.github.kpedal.ui.screens.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DrillHistoryScreen(
    results: List<DrillResult>,
    onBack: () -> Unit,
    onResultClick: (DrillResult) -> Unit,
    onDeleteResult: (Long) -> Unit,
    onDeleteAll: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var resultToDelete by remember { mutableStateOf<DrillResult?>(null) }

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
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onBack() }
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.history),
                        color = Theme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${results.size}",
                        color = Theme.colors.dim,
                        fontSize = 12.sp
                    )
                    if (results.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.clear),
                            color = Theme.colors.problem,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { showClearConfirm = true }
                        )
                    }
                }
            }

            Divider()

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_history),
                        color = Theme.colors.dim,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.id }) { result ->
                        ResultRow(
                            result = result,
                            onClick = { onResultClick(result) },
                            onDelete = { resultToDelete = result }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showClearConfirm) {
            ConfirmDialog(
                title = stringResource(R.string.clear_all_question),
                confirmText = stringResource(R.string.clear),
                onConfirm = {
                    onDeleteAll()
                    showClearConfirm = false
                },
                onDismiss = { showClearConfirm = false }
            )
        }

        resultToDelete?.let { result ->
            ConfirmDialog(
                title = stringResource(R.string.delete_question),
                confirmText = stringResource(R.string.delete),
                onConfirm = {
                    onDeleteResult(result.id)
                    resultToDelete = null
                },
                onDismiss = { resultToDelete = null }
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
private fun ResultRow(
    result: DrillResult,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val scoreColor = when {
        result.score >= 75 -> Theme.colors.optimal
        result.score >= 50 -> Theme.colors.attention
        else -> Theme.colors.problem
    }

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
                Text(
                    text = result.drillName,
                    color = Theme.colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = formatDate(result.timestamp),
                        color = Theme.colors.dim,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${result.score.toInt()}%",
                        color = scoreColor,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "×",
                color = Theme.colors.dim,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(start = 8.dp)
            )
        }
        Divider()
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    confirmText: String,
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
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = Theme.colors.dim,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Text(
                    text = confirmText,
                    color = Theme.colors.problem,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onConfirm() }
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
