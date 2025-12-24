package io.github.kpedal.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.BuildConfig
import io.github.kpedal.data.PreferencesRepository
import io.github.kpedal.ui.theme.Theme

/**
 * Preferences screen - threshold configuration and app info.
 */
@Composable
fun SettingsScreen(
    settings: PreferencesRepository.Settings,
    onBack: () -> Unit,
    onNavigateToAlertSettings: () -> Unit,
    onUpdateBalanceThreshold: (Int) -> Unit,
    onUpdateTeOptimalRange: (Int, Int) -> Unit,
    onUpdatePsMinimum: (Int) -> Unit
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
                text = "Preferences",
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
        ) {
            // Balance threshold
            SectionHeader("BALANCE THRESHOLD")
            ThresholdSelector(
                description = "Deviation from 50/50 to trigger alert",
                options = listOf(2, 3, 5, 7, 10),
                selectedValue = settings.balanceThreshold,
                formatValue = { "±$it%" },
                onChange = onUpdateBalanceThreshold
            )

            // TE threshold
            SectionHeader("TORQUE EFFECTIVENESS")
            ThresholdSelector(
                description = "Minimum TE% to be considered optimal",
                options = listOf(60, 65, 70, 75, 80),
                selectedValue = settings.teOptimalMin,
                formatValue = { "≥$it%" },
                onChange = { onUpdateTeOptimalRange(it, settings.teOptimalMax) }
            )

            // PS threshold
            SectionHeader("PEDAL SMOOTHNESS")
            ThresholdSelector(
                description = "Minimum PS% to be considered optimal",
                options = listOf(15, 20, 25, 30),
                selectedValue = settings.psMinimum,
                formatValue = { "≥$it%" },
                onChange = onUpdatePsMinimum
            )

            // Alerts link
            SectionHeader("ALERTS")
            NavigationRow(
                label = "Configure Alerts",
                subtitle = "When and how to notify",
                onClick = onNavigateToAlertSettings
            )

            // About
            SectionHeader("ABOUT")
            InfoRow("Version", BuildConfig.VERSION_NAME)

            val context = LocalContext.current
            LinkRow(
                label = "Privacy Policy",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kpedal.com/privacy"))
                    context.startActivity(intent)
                }
            )
            LinkRow(
                label = "Contact",
                value = "info@kpedal.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:info@kpedal.com")
                    }
                    context.startActivity(intent)
                }
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
private fun ThresholdSelector(
    description: String,
    options: List<Int>,
    selectedValue: Int,
    formatValue: (Int) -> String,
    onChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = description,
            color = Theme.colors.dim,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedValue == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Theme.colors.optimal else Theme.colors.surface)
                        .clickable { onChange(option) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatValue(option),
                        color = if (isSelected) Theme.colors.background else Theme.colors.dim,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Theme.colors.text,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Theme.colors.text,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Theme.colors.dim,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun LinkRow(
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Theme.colors.text,
            fontSize = 13.sp
        )
        Text(
            text = value ?: "→",
            color = Theme.colors.optimal,
            fontSize = 12.sp
        )
    }
}
