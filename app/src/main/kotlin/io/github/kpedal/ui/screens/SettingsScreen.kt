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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.BuildConfig
import io.github.kpedal.R
import io.github.kpedal.auth.DeviceAuthService
import io.github.kpedal.data.AuthRepository
import io.github.kpedal.data.PreferencesRepository
import io.github.kpedal.data.SyncService
import io.github.kpedal.ui.theme.Theme
import io.github.kpedal.util.LocaleHelper
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
    currentLanguage: LocaleHelper.AppLanguage,
    onBack: () -> Unit,
    onNavigateToAlertSettings: () -> Unit,
    onUpdateBalanceThreshold: (Int) -> Unit,
    onUpdateTeOptimalRange: (Int, Int) -> Unit,
    onUpdatePsMinimum: (Int) -> Unit,
    onStartDeviceAuth: () -> Unit,
    onCancelDeviceAuth: () -> Unit,
    onSignOut: () -> Unit,
    onManualSync: () -> Unit,
    onFullSync: () -> Unit,
    onUpdateBackgroundMode: (Boolean) -> Unit,
    onUpdateAutoSync: (Boolean) -> Unit,
    onUpdateLanguage: (LocaleHelper.AppLanguage) -> Unit,
    onCheckSyncRequest: () -> Unit = {},
    onDeviceRevokedAcknowledged: () -> Unit = {},
    onNavigateToLinkHelp: () -> Unit = {}
) {
    // Check for web sync request when screen opens
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            onCheckSyncRequest()
        }
    }

    // Show dialog when device was revoked
    if (syncState.deviceRevoked) {
        DeviceRevokedDialog(
            onDismiss = onDeviceRevokedAcknowledged
        )
    }

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
                text = stringResource(R.string.preferences),
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
            SectionHeader(stringResource(R.string.account))
            if (authState.isLoggedIn) {
                AccountRow(
                    email = authState.userEmail ?: "",
                    name = authState.userName ?: "",
                    isSyncing = syncState.status == SyncService.SyncStatus.SYNCING,
                    onSync = onFullSync,
                    onSignOut = onSignOut
                )
            } else {
                DeviceAuthSection(
                    deviceAuthState = deviceAuthState,
                    onStartAuth = onStartDeviceAuth,
                    onCancel = onCancelDeviceAuth,
                    onNavigateToLinkHelp = onNavigateToLinkHelp
                )
            }

            // Sync section (only show if logged in)
            if (authState.isLoggedIn) {
                SectionHeader(stringResource(R.string.settings_sync))
                ToggleRow(
                    label = stringResource(R.string.settings_auto_sync),
                    subtitle = stringResource(R.string.settings_auto_sync_desc),
                    enabled = autoSyncEnabled,
                    onToggle = onUpdateAutoSync
                )
                SyncStatusRow(
                    syncState = syncState,
                    onManualSync = onManualSync
                )
            }

            // Data Collection section
            SectionHeader(stringResource(R.string.settings_data_collection))
            ToggleRow(
                label = stringResource(R.string.background_mode),
                subtitle = stringResource(R.string.background_mode_desc),
                enabled = backgroundModeEnabled,
                onToggle = onUpdateBackgroundMode
            )

            // Language section (visible on first screen)
            SectionHeader(stringResource(R.string.language).uppercase())
            LanguageRow(
                currentLanguage = currentLanguage,
                onSelectLanguage = onUpdateLanguage
            )

            // Balance threshold
            SectionHeader(stringResource(R.string.settings_balance_threshold))
            ThresholdSelector(
                description = stringResource(R.string.settings_balance_threshold_desc),
                options = listOf(2, 3, 5, 7, 10),
                selectedValue = settings.balanceThreshold,
                formatValue = { "±$it%" },
                onChange = onUpdateBalanceThreshold
            )

            // TE threshold
            SectionHeader(stringResource(R.string.settings_te))
            ThresholdSelector(
                description = stringResource(R.string.settings_te_desc),
                options = listOf(60, 65, 70, 75, 80),
                selectedValue = settings.teOptimalMin,
                formatValue = { "≥$it%" },
                onChange = { onUpdateTeOptimalRange(it, settings.teOptimalMax) }
            )

            // PS threshold
            SectionHeader(stringResource(R.string.settings_ps))
            ThresholdSelector(
                description = stringResource(R.string.settings_ps_desc),
                options = listOf(15, 20, 25, 30),
                selectedValue = settings.psMinimum,
                formatValue = { "≥$it%" },
                onChange = onUpdatePsMinimum
            )

            // Alerts link
            SectionHeader(stringResource(R.string.alerts))
            NavigationRow(
                label = stringResource(R.string.settings_configure_alerts),
                subtitle = stringResource(R.string.settings_configure_alerts_desc),
                onClick = onNavigateToAlertSettings
            )

            // About
            SectionHeader(stringResource(R.string.settings_about))
            InfoRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)

            val context = LocalContext.current
            LinkRow(
                label = stringResource(R.string.settings_privacy),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kpedal.com/privacy"))
                    context.startActivity(intent)
                }
            )
            LinkRow(
                label = stringResource(R.string.settings_contact),
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
    isSyncing: Boolean,
    onSync: () -> Unit,
    onSignOut: () -> Unit
) {
    val linkedAccountText = stringResource(R.string.settings_linked_account)
    val syncText = stringResource(R.string.sync)
    val signOutText = stringResource(R.string.sign_out)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Account info card with sync button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Theme.colors.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifEmpty { linkedAccountText },
                    color = Theme.colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = email,
                    color = Theme.colors.dim,
                    fontSize = 12.sp
                )
            }

            // Sync button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Theme.colors.optimal.copy(alpha = 0.15f))
                    .clickable(enabled = !isSyncing) { onSync() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Theme.colors.optimal,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = syncText,
                        color = Theme.colors.optimal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sign Out button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.problem.copy(alpha = 0.12f))
                .clickable { onSignOut() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = signOutText,
                color = Theme.colors.problem,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DeviceAuthSection(
    deviceAuthState: DeviceAuthService.DeviceAuthState,
    onStartAuth: () -> Unit,
    onCancel: () -> Unit,
    onNavigateToLinkHelp: () -> Unit
) {
    val context = LocalContext.current

    // Pre-load all strings
    val linkAccountText = stringResource(R.string.link_account)
    val syncDescText = stringResource(R.string.settings_sync_desc)
    val gettingCodeText = stringResource(R.string.getting_code)
    val enterCodeText = stringResource(R.string.settings_enter_code)
    val waitingAuthText = stringResource(R.string.waiting_auth)
    val cancelText = stringResource(R.string.cancel)
    val accountLinkedText = stringResource(R.string.account_linked)
    val connectionFailedText = stringResource(R.string.settings_connection_failed)
    val connectionFailedDescText = stringResource(R.string.settings_connection_failed_desc)
    val tryAgainText = stringResource(R.string.settings_try_again)
    val codeExpiredText = stringResource(R.string.settings_code_expired)
    val codeExpiredDescText = stringResource(R.string.settings_code_expired_desc)
    val getNewCodeText = stringResource(R.string.settings_get_new_code)
    val accessDeniedText = stringResource(R.string.settings_access_denied)
    val accessDeniedDescText = stringResource(R.string.settings_access_denied_desc)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        when (deviceAuthState) {
            is DeviceAuthService.DeviceAuthState.Idle -> {
                // Show "Link Account" button - prominent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Theme.colors.optimal)
                        .clickable { onStartAuth() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = linkAccountText,
                            color = Theme.colors.background,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = syncDescText,
                            color = Theme.colors.background.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // "Why & how sync to cloud?" link - styled as clickable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Theme.colors.surface)
                        .clickable { onNavigateToLinkHelp() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.why_how_sync),
                        color = Theme.colors.optimal,
                        fontSize = 12.sp
                    )
                }
            }

            is DeviceAuthService.DeviceAuthState.RequestingCode -> {
                // Loading state - card style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Theme.colors.surface)
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Theme.colors.optimal,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = gettingCodeText,
                            color = Theme.colors.dim,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            is DeviceAuthService.DeviceAuthState.WaitingForUser,
            is DeviceAuthService.DeviceAuthState.Polling -> {
                val userCode = when (deviceAuthState) {
                    is DeviceAuthService.DeviceAuthState.WaitingForUser -> deviceAuthState.userCode
                    is DeviceAuthService.DeviceAuthState.Polling -> deviceAuthState.userCode
                    else -> ""
                }
                val verificationUri = when (deviceAuthState) {
                    is DeviceAuthService.DeviceAuthState.WaitingForUser -> deviceAuthState.verificationUri
                    is DeviceAuthService.DeviceAuthState.Polling -> deviceAuthState.verificationUri
                    else -> "link.kpedal.com"
                }

                // Card with code display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Theme.colors.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = enterCodeText,
                        color = Theme.colors.dim,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = verificationUri,
                        color = Theme.colors.optimal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://$verificationUri"))
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large code display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Theme.colors.background)
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userCode,
                            color = Theme.colors.text,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (deviceAuthState is DeviceAuthService.DeviceAuthState.Polling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Theme.colors.attention,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = waitingAuthText,
                            color = Theme.colors.attention,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cancel button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Theme.colors.surface)
                        .clickable { onCancel() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cancelText,
                        color = Theme.colors.dim,
                        fontSize = 13.sp
                    )
                }
            }

            is DeviceAuthService.DeviceAuthState.Success -> {
                // Success state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Theme.colors.optimal.copy(alpha = 0.15f))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✓",
                            color = Theme.colors.optimal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = accountLinkedText,
                            color = Theme.colors.optimal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            is DeviceAuthService.DeviceAuthState.Error -> {
                ErrorStateCard(
                    title = connectionFailedText,
                    subtitle = connectionFailedDescText,
                    buttonText = tryAgainText,
                    onAction = onStartAuth
                )
            }

            is DeviceAuthService.DeviceAuthState.Expired -> {
                ErrorStateCard(
                    title = codeExpiredText,
                    subtitle = codeExpiredDescText,
                    buttonText = getNewCodeText,
                    onAction = onStartAuth
                )
            }

            is DeviceAuthService.DeviceAuthState.AccessDenied -> {
                ErrorStateCard(
                    title = accessDeniedText,
                    subtitle = accessDeniedDescText,
                    buttonText = tryAgainText,
                    onAction = onStartAuth
                )
            }
        }
    }
}

@Composable
private fun ErrorStateCard(
    title: String,
    subtitle: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error message - 2 lines
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Theme.colors.problem.copy(alpha = 0.12f))
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Theme.colors.problem,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Theme.colors.problem.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Retry button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.optimal)
                .clickable { onAction() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = Theme.colors.background,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
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
    val neverSyncedText = stringResource(R.string.settings_never_synced)
    val syncingText = stringResource(R.string.syncing)
    val syncedText = stringResource(R.string.synced)
    val syncFailedText = stringResource(R.string.sync_failed)
    val offlineText = stringResource(R.string.offline)
    val upToDateText = stringResource(R.string.settings_up_to_date)
    val syncNowText = stringResource(R.string.settings_sync_now)

    val lastSyncText = if (syncState.lastSyncTimestamp > 0) {
        val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        stringResource(R.string.settings_last_sync, formatter.format(Date(syncState.lastSyncTimestamp)))
    } else {
        neverSyncedText
    }

    val statusText = when (syncState.status) {
        SyncService.SyncStatus.SYNCING -> syncingText
        SyncService.SyncStatus.SUCCESS -> syncedText
        SyncService.SyncStatus.FAILED -> syncFailedText
        SyncService.SyncStatus.OFFLINE -> offlineText
        SyncService.SyncStatus.IDLE -> if (syncState.pendingCount > 0) stringResource(R.string.settings_pending, syncState.pendingCount) else upToDateText
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
                text = syncNowText,
                color = Theme.colors.optimal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onManualSync() }
            )
        }
    }
}

/**
 * Dialog shown when device access was revoked from web.
 */
@Composable
private fun DeviceRevokedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.device_disconnected),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.settings_device_revoked_desc),
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.ok),
                    color = Theme.colors.optimal,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = Theme.colors.surface,
        titleContentColor = Theme.colors.text,
        textContentColor = Theme.colors.dim
    )
}

/**
 * Language selection row with dropdown menu.
 */
@Composable
private fun LanguageRow(
    currentLanguage: LocaleHelper.AppLanguage,
    onSelectLanguage: (LocaleHelper.AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val languageDisplayName = when (currentLanguage) {
        LocaleHelper.AppLanguage.SYSTEM -> stringResource(R.string.language_system)
        LocaleHelper.AppLanguage.ENGLISH -> stringResource(R.string.language_english)
        LocaleHelper.AppLanguage.SPANISH -> stringResource(R.string.language_spanish)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.language),
            color = Theme.colors.text,
            fontSize = 13.sp
        )

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Theme.colors.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = languageDisplayName,
                    color = Theme.colors.optimal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Theme.colors.optimal,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Theme.colors.surface)
            ) {
                LocaleHelper.AppLanguage.entries.forEach { language ->
                    val displayName = when (language) {
                        LocaleHelper.AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                        LocaleHelper.AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                        LocaleHelper.AppLanguage.SPANISH -> stringResource(R.string.language_spanish)
                    }

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (language == currentLanguage) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Theme.colors.optimal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (language == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                                    color = if (language == currentLanguage) Theme.colors.optimal else Theme.colors.text
                                )
                            }
                        },
                        onClick = {
                            onSelectLanguage(language)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
