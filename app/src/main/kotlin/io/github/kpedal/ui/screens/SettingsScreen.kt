package io.github.kpedal.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.BuildConfig
import io.github.kpedal.auth.DeviceAuthService
import io.github.kpedal.data.AuthRepository
import io.github.kpedal.data.PreferencesRepository
import io.github.kpedal.data.SyncService
import io.github.kpedal.ui.theme.Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Preferences screen - threshold configuration and app info.
 */
@Composable
fun SettingsScreen(
    settings: PreferencesRepository.Settings,
    authState: AuthRepository.AuthState,
    syncState: SyncService.SyncState,
    deviceAuthState: DeviceAuthService.DeviceAuthState,
    backgroundModeEnabled: Boolean,
    autoSyncEnabled: Boolean,
    onBack: () -> Unit,
    onNavigateToAlertSettings: () -> Unit,
    onUpdateBalanceThreshold: (Int) -> Unit,
    onUpdateTeOptimalRange: (Int, Int) -> Unit,
    onUpdatePsMinimum: (Int) -> Unit,
    onStartDeviceAuth: () -> Unit,
    onCancelDeviceAuth: () -> Unit,
    onSignOut: () -> Unit,
    onManualSync: () -> Unit,
    onUpdateBackgroundMode: (Boolean) -> Unit,
    onUpdateAutoSync: (Boolean) -> Unit
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
            // Account section
            SectionHeader("ACCOUNT")
            if (authState.isLoggedIn) {
                AccountRow(
                    email = authState.userEmail ?: "",
                    name = authState.userName ?: "",
                    onSignOut = onSignOut
                )
            } else {
                DeviceAuthSection(
                    deviceAuthState = deviceAuthState,
                    onStartAuth = onStartDeviceAuth,
                    onCancel = onCancelDeviceAuth
                )
            }

            // Sync section (only show if logged in)
            if (authState.isLoggedIn) {
                SectionHeader("SYNC")
                ToggleRow(
                    label = "Auto-sync rides",
                    subtitle = "Sync after each ride",
                    enabled = autoSyncEnabled,
                    onToggle = onUpdateAutoSync
                )
                SyncStatusRow(
                    syncState = syncState,
                    onManualSync = onManualSync
                )
            }

            // Data Collection section
            SectionHeader("DATA COLLECTION")
            ToggleRow(
                label = "Background Mode",
                subtitle = "Collect data for all rides",
                enabled = backgroundModeEnabled,
                onToggle = onUpdateBackgroundMode
            )

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

@Composable
private fun AccountRow(
    email: String,
    name: String,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Theme.colors.optimal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Theme.colors.background,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Theme.colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = email,
                color = Theme.colors.dim,
                fontSize = 11.sp
            )
        }

        Text(
            text = "Sign Out",
            color = Theme.colors.problem,
            fontSize = 12.sp,
            modifier = Modifier.clickable { onSignOut() }
        )
    }
}

@Composable
private fun DeviceAuthSection(
    deviceAuthState: DeviceAuthService.DeviceAuthState,
    onStartAuth: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    when (deviceAuthState) {
        is DeviceAuthService.DeviceAuthState.Idle -> {
            // Show "Link Account" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartAuth() }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Link Account",
                        color = Theme.colors.text,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Sync rides to kpedal.com",
                        color = Theme.colors.dim,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = ">",
                    color = Theme.colors.optimal,
                    fontSize = 16.sp
                )
            }
        }

        is DeviceAuthService.DeviceAuthState.RequestingCode -> {
            // Loading state
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Theme.colors.optimal,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Getting code...",
                    color = Theme.colors.dim,
                    fontSize = 13.sp
                )
            }
        }

        is DeviceAuthService.DeviceAuthState.WaitingForUser,
        is DeviceAuthService.DeviceAuthState.Polling -> {
            // Show the code and instructions
            val userCode = when (deviceAuthState) {
                is DeviceAuthService.DeviceAuthState.WaitingForUser -> deviceAuthState.userCode
                is DeviceAuthService.DeviceAuthState.Polling -> deviceAuthState.userCode
                else -> ""
            }
            val verificationUri = when (deviceAuthState) {
                is DeviceAuthService.DeviceAuthState.WaitingForUser -> deviceAuthState.verificationUri
                is DeviceAuthService.DeviceAuthState.Polling -> deviceAuthState.verificationUri
                else -> "kpedal.com/link"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter this code at:",
                    color = Theme.colors.dim,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = verificationUri,
                    color = Theme.colors.optimal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://$verificationUri"))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Code display
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Theme.colors.surface)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = userCode,
                        color = Theme.colors.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (deviceAuthState is DeviceAuthService.DeviceAuthState.Polling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Theme.colors.attention,
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "Waiting for authorization...",
                        color = Theme.colors.attention,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cancel",
                    color = Theme.colors.problem,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onCancel() }
                )
            }
        }

        is DeviceAuthService.DeviceAuthState.Success -> {
            // Briefly show success (will transition to logged in state)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Linked!",
                    color = Theme.colors.optimal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        is DeviceAuthService.DeviceAuthState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = deviceAuthState.message,
                    color = Theme.colors.problem,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try Again",
                    color = Theme.colors.optimal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onStartAuth() }
                )
            }
        }

        is DeviceAuthService.DeviceAuthState.Expired -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Code expired",
                    color = Theme.colors.problem,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Get New Code",
                    color = Theme.colors.optimal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onStartAuth() }
                )
            }
        }

        is DeviceAuthService.DeviceAuthState.AccessDenied -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Access denied",
                    color = Theme.colors.problem,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try Again",
                    color = Theme.colors.optimal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onStartAuth() }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Theme.colors.background,
                checkedTrackColor = Theme.colors.optimal,
                uncheckedThumbColor = Theme.colors.dim,
                uncheckedTrackColor = Theme.colors.surface
            )
        )
    }
}

@Composable
private fun SyncStatusRow(
    syncState: SyncService.SyncState,
    onManualSync: () -> Unit
) {
    val lastSyncText = if (syncState.lastSyncTimestamp > 0) {
        val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        "Last sync: ${formatter.format(Date(syncState.lastSyncTimestamp))}"
    } else {
        "Never synced"
    }

    val statusText = when (syncState.status) {
        SyncService.SyncStatus.SYNCING -> "Syncing..."
        SyncService.SyncStatus.SUCCESS -> "Synced"
        SyncService.SyncStatus.FAILED -> "Sync failed"
        SyncService.SyncStatus.OFFLINE -> "Offline"
        SyncService.SyncStatus.IDLE -> if (syncState.pendingCount > 0) "${syncState.pendingCount} pending" else "Up to date"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusText,
                color = when (syncState.status) {
                    SyncService.SyncStatus.SYNCING -> Theme.colors.attention
                    SyncService.SyncStatus.SUCCESS -> Theme.colors.optimal
                    SyncService.SyncStatus.FAILED -> Theme.colors.problem
                    SyncService.SyncStatus.OFFLINE -> Theme.colors.dim
                    SyncService.SyncStatus.IDLE -> if (syncState.pendingCount > 0) Theme.colors.attention else Theme.colors.dim
                },
                fontSize = 12.sp
            )
            Text(
                text = lastSyncText,
                color = Theme.colors.muted,
                fontSize = 10.sp
            )
        }
        if (syncState.pendingCount > 0 && syncState.status != SyncService.SyncStatus.SYNCING) {
            Text(
                text = "Sync Now",
                color = Theme.colors.optimal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onManualSync() }
            )
        }
    }
}
