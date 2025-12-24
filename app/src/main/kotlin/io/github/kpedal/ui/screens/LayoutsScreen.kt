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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.ui.theme.Theme

/**
 * Layouts screen - Karoo data field layouts.
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
                text = "Data Field Layouts",
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
            Text(
                text = "Tap to preview layout on Karoo",
                color = Theme.colors.dim,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LayoutRow(
                name = "Quick Glance",
                description = "Status + balance bar",
                onClick = { onNavigate("quick-glance") }
            )
            LayoutRow(
                name = "Power + Balance",
                description = "Large L/R focus",
                onClick = { onNavigate("power-balance") }
            )
            LayoutRow(
                name = "Efficiency",
                description = "TE and PS metrics",
                onClick = { onNavigate("efficiency") }
            )
            LayoutRow(
                name = "Full Overview",
                description = "All metrics compact",
                onClick = { onNavigate("full-overview") }
            )
            LayoutRow(
                name = "Balance Trend",
                description = "3s/10s smoothed",
                onClick = { onNavigate("balance-trend") }
            )
            LayoutRow(
                name = "Single Balance",
                description = "Balance only",
                onClick = { onNavigate("single-balance") }
            )

            Spacer(modifier = Modifier.height(8.dp))
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
