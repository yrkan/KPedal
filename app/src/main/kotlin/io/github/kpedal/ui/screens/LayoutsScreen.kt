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
import io.github.kpedal.ui.theme.Theme

/**
 * Layouts screen - Karoo data field layouts.
 * Shows all 19 DataTypes grouped by category.
 */
@Composable
fun LayoutsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
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
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.data_field_layouts),
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
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.tap_to_preview),
                color = Theme.colors.dim,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // CORE - Essential layouts
            CategoryHeader(stringResource(R.string.layout_category_core))

            LayoutRow(
                name = stringResource(R.string.datatype_quick_glance),
                description = stringResource(R.string.layout_quick_glance_short),
                onClick = { onNavigate("quick-glance") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_full_overview),
                description = stringResource(R.string.layout_full_overview_short),
                onClick = { onNavigate("full-overview") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_live),
                description = stringResource(R.string.layout_live_short),
                onClick = { onNavigate("live") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_efficiency),
                description = stringResource(R.string.layout_efficiency_short),
                onClick = { onNavigate("efficiency") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_balance_trend),
                description = stringResource(R.string.layout_balance_trend_short),
                onClick = { onNavigate("balance-trend") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_single_balance),
                description = stringResource(R.string.layout_single_balance_short),
                onClick = { onNavigate("single-balance") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_compact_multi),
                description = stringResource(R.string.layout_compact_multi_short),
                onClick = { onNavigate("compact-multi") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ANALYSIS - Score and trends
            CategoryHeader(stringResource(R.string.layout_category_analysis))

            LayoutRow(
                name = stringResource(R.string.datatype_pedaling_score),
                description = stringResource(R.string.layout_pedaling_score_short),
                onClick = { onNavigate("pedaling-score") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_fatigue_indicator),
                description = stringResource(R.string.layout_fatigue_indicator_short),
                onClick = { onNavigate("fatigue-indicator") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_delta_average),
                description = stringResource(R.string.layout_delta_average_short),
                onClick = { onNavigate("delta-average") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_symmetry_index),
                description = stringResource(R.string.layout_symmetry_index_short),
                onClick = { onNavigate("symmetry-index") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // FOCUS - Single leg / power focus
            CategoryHeader(stringResource(R.string.layout_category_focus))

            LayoutRow(
                name = stringResource(R.string.datatype_left_leg),
                description = stringResource(R.string.layout_left_leg_short),
                onClick = { onNavigate("left-leg") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_right_leg),
                description = stringResource(R.string.layout_right_leg_short),
                onClick = { onNavigate("right-leg") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_power_balance),
                description = stringResource(R.string.layout_power_balance_short),
                onClick = { onNavigate("power-balance") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_power_focus),
                description = stringResource(R.string.layout_power_focus_short),
                onClick = { onNavigate("power-focus") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WITH CONTEXT - Combined with other metrics
            CategoryHeader(stringResource(R.string.layout_category_context))

            LayoutRow(
                name = stringResource(R.string.datatype_cadence_balance),
                description = stringResource(R.string.layout_cadence_balance_short),
                onClick = { onNavigate("cadence-balance") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_hr_efficiency),
                description = stringResource(R.string.layout_hr_efficiency_short),
                onClick = { onNavigate("hr-efficiency") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_climbing_mode),
                description = stringResource(R.string.layout_climbing_mode_short),
                onClick = { onNavigate("climbing-mode") }
            )
            LayoutRow(
                name = stringResource(R.string.datatype_sprint_mode),
                description = stringResource(R.string.layout_sprint_mode_short),
                onClick = { onNavigate("sprint-mode") }
            )

            Spacer(modifier = Modifier.height(20.dp))
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
private fun CategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Theme.colors.dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun LayoutRow(
    name: String,
    description: String,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Theme.colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Theme.colors.dim,
                fontSize = 10.sp
            )
        }
        Text(
            text = "›",
            color = Theme.colors.dim,
            fontSize = 16.sp
        )
    }
}
