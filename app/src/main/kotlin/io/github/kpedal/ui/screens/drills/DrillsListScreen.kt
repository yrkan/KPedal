package io.github.kpedal.ui.screens.drills

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
import io.github.kpedal.drill.DrillCatalog
import io.github.kpedal.drill.model.*
import io.github.kpedal.ui.theme.Theme

@Composable
fun DrillsListScreen(
    customDrills: List<Drill> = emptyList(),
    onBack: () -> Unit,
    onDrillClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onCreateCustomDrill: () -> Unit = {},
    onDeleteCustomDrill: (String) -> Unit = {},
    onNavigateToLive: () -> Unit = {}
) {
    val drills = DrillCatalog.all

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
                // Tabs: Live | Drills
                Text(
                    text = stringResource(R.string.live),
                    color = Theme.colors.dim,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigateToLive() }
                )
                Text(
                    text = " · ",
                    color = Theme.colors.dim,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(R.string.drills),
                    color = Theme.colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(R.string.drill_history),
                color = Theme.colors.dim,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onHistoryClick() }
            )
        }

        Divider()

        // List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Custom drills section
            if (customDrills.isNotEmpty()) {
                SectionHeader(stringResource(R.string.my_drills))
                customDrills.forEach { drill ->
                    DrillRow(
                        drill = drill,
                        isCustom = true,
                        onClick = { onDrillClick(drill.id) },
                        onDelete = { onDeleteCustomDrill(drill.id) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Create custom drill button
            CreateDrillButton(onClick = onCreateCustomDrill)

            Spacer(modifier = Modifier.height(12.dp))

            // Built-in drills
            SectionHeader(stringResource(R.string.built_in_drills))
            drills.forEach { drill ->
                DrillRow(drill = drill, onClick = { onDrillClick(drill.id) })
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Theme.colors.dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun CreateDrillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.optimal.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "+",
            color = Theme.colors.optimal,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.create_custom_drill),
            color = Theme.colors.optimal,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
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
private fun DrillRow(
    drill: Drill,
    isCustom: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val metricColor = when (drill.metric) {
        DrillMetric.BALANCE -> Theme.colors.attention
        DrillMetric.TORQUE_EFFECTIVENESS -> Theme.colors.optimal
        DrillMetric.PEDAL_SMOOTHNESS -> Theme.colors.problem
        DrillMetric.COMBINED -> Theme.colors.text
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .background(metricColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = drill.nameOverride ?: stringResource(drill.nameRes),
                            color = Theme.colors.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.custom),
                                color = Theme.colors.muted,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = drill.durationFormatted,
                        color = Theme.colors.dim,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCustom && onDelete != null) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = Theme.colors.problem,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onDelete() }
                            .padding(horizontal = 8.dp)
                    )
                }
                Text(
                    text = "›",
                    color = Theme.colors.dim,
                    fontSize = 16.sp
                )
            }
        }
        Divider()
    }
}
