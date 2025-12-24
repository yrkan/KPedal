package io.github.kpedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.data.AlertSettings
import io.github.kpedal.data.AlertTriggerLevel
import io.github.kpedal.data.MetricAlertConfig
import io.github.kpedal.ui.theme.Theme

/**
 * Alert Settings screen - simplified design.
 */
@Composable
fun AlertSettingsScreen(
    alertSettings: AlertSettings,
    onBack: () -> Unit,
    onUpdateGlobalEnabled: (Boolean) -> Unit,
    onUpdateScreenWake: (Boolean) -> Unit,
    onUpdateBalanceConfig: (MetricAlertConfig) -> Unit,
    onUpdateTeConfig: (MetricAlertConfig) -> Unit,
    onUpdatePsConfig: (MetricAlertConfig) -> Unit
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
                text = "Alerts",
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
            Spacer(modifier = Modifier.height(12.dp))

            // Master toggle
            ToggleRow(
                label = "Enable alerts",
                checked = alertSettings.globalEnabled,
                onCheckedChange = { onUpdateGlobalEnabled(it) }
            )

            if (alertSettings.globalEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                // Which metrics
                SectionLabel("METRICS TO MONITOR")
                Spacer(modifier = Modifier.height(6.dp))

                MetricCheckRow(
                    name = "Balance",
                    color = Theme.colors.attention,
                    enabled = alertSettings.balanceConfig.triggerLevel != AlertTriggerLevel.DISABLED,
                    onToggle = {
                        val newLevel = if (alertSettings.balanceConfig.triggerLevel == AlertTriggerLevel.DISABLED) {
                            alertSettings.teConfig.triggerLevel.takeIf { it != AlertTriggerLevel.DISABLED }
                                ?: AlertTriggerLevel.PROBLEM_ONLY
                        } else AlertTriggerLevel.DISABLED
                        onUpdateBalanceConfig(alertSettings.balanceConfig.copy(triggerLevel = newLevel))
                    }
                )
                MetricCheckRow(
                    name = "Torque Effectiveness",
                    color = Theme.colors.optimal,
                    enabled = alertSettings.teConfig.triggerLevel != AlertTriggerLevel.DISABLED,
                    onToggle = {
                        val newLevel = if (alertSettings.teConfig.triggerLevel == AlertTriggerLevel.DISABLED) {
                            alertSettings.balanceConfig.triggerLevel.takeIf { it != AlertTriggerLevel.DISABLED }
                                ?: AlertTriggerLevel.PROBLEM_ONLY
                        } else AlertTriggerLevel.DISABLED
                        onUpdateTeConfig(alertSettings.teConfig.copy(triggerLevel = newLevel))
                    }
                )
                MetricCheckRow(
                    name = "Pedal Smoothness",
                    color = Theme.colors.problem,
                    enabled = alertSettings.psConfig.triggerLevel != AlertTriggerLevel.DISABLED,
                    onToggle = {
                        val newLevel = if (alertSettings.psConfig.triggerLevel == AlertTriggerLevel.DISABLED) {
                            alertSettings.balanceConfig.triggerLevel.takeIf { it != AlertTriggerLevel.DISABLED }
                                ?: AlertTriggerLevel.PROBLEM_ONLY
                        } else AlertTriggerLevel.DISABLED
                        onUpdatePsConfig(alertSettings.psConfig.copy(triggerLevel = newLevel))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sensitivity (global)
                SectionLabel("SENSITIVITY")
                Spacer(modifier = Modifier.height(6.dp))

                val currentLevel = alertSettings.balanceConfig.triggerLevel
                    .takeIf { it != AlertTriggerLevel.DISABLED }
                    ?: alertSettings.teConfig.triggerLevel.takeIf { it != AlertTriggerLevel.DISABLED }
                    ?: alertSettings.psConfig.triggerLevel.takeIf { it != AlertTriggerLevel.DISABLED }
                    ?: AlertTriggerLevel.PROBLEM_ONLY

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SensitivityChip(
                        icon = Theme.colors.problem,
                        label = "Critical",
                        selected = currentLevel == AlertTriggerLevel.PROBLEM_ONLY,
                        onClick = {
                            if (alertSettings.balanceConfig.triggerLevel != AlertTriggerLevel.DISABLED)
                                onUpdateBalanceConfig(alertSettings.balanceConfig.copy(triggerLevel = AlertTriggerLevel.PROBLEM_ONLY))
                            if (alertSettings.teConfig.triggerLevel != AlertTriggerLevel.DISABLED)
                                onUpdateTeConfig(alertSettings.teConfig.copy(triggerLevel = AlertTriggerLevel.PROBLEM_ONLY))
                            if (alertSettings.psConfig.triggerLevel != AlertTriggerLevel.DISABLED)
                                onUpdatePsConfig(alertSettings.psConfig.copy(triggerLevel = AlertTriggerLevel.PROBLEM_ONLY))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SensitivityChip(
                        icon = Theme.colors.attention,
                        iconSecondary = Theme.colors.problem,
                        label = "Sensitive",
                        selected = currentLevel == AlertTriggerLevel.ATTENTION_AND_UP,
                        onClick = {
                            if (alertSettings.balanceConfig.triggerLevel != AlertTriggerLevel.DISABLED)
                                onUpdateBalanceConfig(alertSettings.balanceConfig.copy(triggerLevel = AlertTriggerLevel.ATTENTION_AND_UP))
                            if (alertSettings.teConfig.triggerLevel != AlertTriggerLevel.DISABLED)
                                onUpdateTeConfig(alertSettings.teConfig.copy(triggerLevel = AlertTriggerLevel.ATTENTION_AND_UP))
                            if (alertSettings.psConfig.triggerLevel != AlertTriggerLevel.DISABLED)
                                onUpdatePsConfig(alertSettings.psConfig.copy(triggerLevel = AlertTriggerLevel.ATTENTION_AND_UP))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notification type
                SectionLabel("NOTIFY VIA")
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MethodChip(
                        label = "Vibrate",
                        enabled = alertSettings.balanceConfig.vibrationAlert,
                        onClick = {
                            val newValue = !alertSettings.balanceConfig.vibrationAlert
                            onUpdateBalanceConfig(alertSettings.balanceConfig.copy(vibrationAlert = newValue))
                            onUpdateTeConfig(alertSettings.teConfig.copy(vibrationAlert = newValue))
                            onUpdatePsConfig(alertSettings.psConfig.copy(vibrationAlert = newValue))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MethodChip(
                        label = "Sound",
                        enabled = alertSettings.balanceConfig.soundAlert,
                        onClick = {
                            val newValue = !alertSettings.balanceConfig.soundAlert
                            onUpdateBalanceConfig(alertSettings.balanceConfig.copy(soundAlert = newValue))
                            onUpdateTeConfig(alertSettings.teConfig.copy(soundAlert = newValue))
                            onUpdatePsConfig(alertSettings.psConfig.copy(soundAlert = newValue))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MethodChip(
                        label = "Banner",
                        enabled = alertSettings.balanceConfig.visualAlert,
                        onClick = {
                            val newValue = !alertSettings.balanceConfig.visualAlert
                            onUpdateBalanceConfig(alertSettings.balanceConfig.copy(visualAlert = newValue))
                            onUpdateTeConfig(alertSettings.teConfig.copy(visualAlert = newValue))
                            onUpdatePsConfig(alertSettings.psConfig.copy(visualAlert = newValue))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Options
                SectionLabel("OPTIONS")
                Spacer(modifier = Modifier.height(6.dp))

                ToggleRow(
                    label = "Wake screen",
                    checked = alertSettings.screenWakeOnAlert,
                    onCheckedChange = onUpdateScreenWake
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cooldown
                CooldownRow(
                    seconds = alertSettings.balanceConfig.cooldownSeconds,
                    onChange = { newCooldown ->
                        onUpdateBalanceConfig(alertSettings.balanceConfig.copy(cooldownSeconds = newCooldown))
                        onUpdateTeConfig(alertSettings.teConfig.copy(cooldownSeconds = newCooldown))
                        onUpdatePsConfig(alertSettings.psConfig.copy(cooldownSeconds = newCooldown))
                    }
                )
            }

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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Theme.colors.dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surface)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Theme.colors.text,
            fontSize = 13.sp
        )
        Toggle(checked = checked)
    }
}

@Composable
private fun Toggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (checked) Theme.colors.optimal else Theme.colors.muted)
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .offset(x = if (checked) 18.dp else 0.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Theme.colors.text)
        )
    }
}

@Composable
private fun MetricCheckRow(
    name: String,
    color: Color,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (enabled) Theme.colors.optimal else Theme.colors.muted),
            contentAlignment = Alignment.Center
        ) {
            if (enabled) {
                Text(
                    text = "✓",
                    color = Theme.colors.background,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        // Color dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = if (enabled) Theme.colors.text else Theme.colors.dim,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SensitivityChip(
    icon: Color,
    iconSecondary: Color? = null,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Theme.colors.surface else Theme.colors.background)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Color indicators
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(icon, CircleShape)
            )
            if (iconSecondary != null) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(iconSecondary, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) Theme.colors.text else Theme.colors.dim,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MethodChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) Theme.colors.optimal else Theme.colors.surface)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Theme.colors.background else Theme.colors.dim,
            fontSize = 11.sp,
            fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CooldownRow(
    seconds: Int,
    onChange: (Int) -> Unit
) {
    val options = listOf(15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cooldown",
            color = Theme.colors.text,
            fontSize = 12.sp,
            modifier = Modifier.width(70.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = seconds == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) Theme.colors.optimal else Theme.colors.background)
                        .clickable { onChange(value) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Theme.colors.background else Theme.colors.dim,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
