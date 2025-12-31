package io.github.kpedal.ui.screens.drills

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.data.database.CustomDrillEntity
import io.github.kpedal.ui.theme.Theme

/**
 * Screen for creating or editing a custom drill.
 * Clean, vertical layout optimized for Karoo 3 (480×800px).
 */
@Composable
fun CustomDrillScreen(
    existingDrill: CustomDrillEntity?,
    onSave: (CustomDrillEntity) -> Unit,
    onBack: () -> Unit
) {
    val isEdit = existingDrill != null

    var name by remember { mutableStateOf(existingDrill?.name ?: "") }
    var metric by remember { mutableStateOf(existingDrill?.metric ?: "BALANCE") }
    var durationMinutes by remember { mutableStateOf((existingDrill?.durationSeconds ?: 60) / 60) }
    var durationSeconds by remember { mutableStateOf((existingDrill?.durationSeconds ?: 60) % 60) }
    var targetType by remember { mutableStateOf(existingDrill?.targetType ?: "RANGE") }
    var targetMin by remember { mutableStateOf(existingDrill?.targetMin ?: getDefaultMin(metric)) }
    var targetMax by remember { mutableStateOf(existingDrill?.targetMax ?: getDefaultMax(metric)) }

    val totalSeconds = durationMinutes * 60 + durationSeconds
    val canSave = name.isNotBlank() && totalSeconds >= 10

    val metricNameLocalized = when (metric) {
        "BALANCE" -> stringResource(R.string.balance_option)
        "TE" -> stringResource(R.string.torque_eff_short)
        "PS" -> stringResource(R.string.smoothness_short)
        else -> metric
    }
    val drillDescription = stringResource(R.string.custom_drill_desc, metricNameLocalized)

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
            Text(
                text = "←",
                color = Theme.colors.dim,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() }
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(if (isEdit) R.string.edit_drill else R.string.new_drill),
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.save),
                color = if (canSave) Theme.colors.optimal else Theme.colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(
                        enabled = canSave,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val entity = CustomDrillEntity(
                            id = existingDrill?.id ?: 0,
                            name = name.trim(),
                            description = drillDescription,
                            metric = metric,
                            durationSeconds = totalSeconds,
                            targetType = targetType,
                            targetMin = if (targetType in listOf("MIN", "RANGE")) targetMin else null,
                            targetMax = if (targetType in listOf("MAX", "RANGE")) targetMax else null,
                            targetValue = null,
                            tolerance = 2f,
                            createdAt = existingDrill?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(entity)
                    }
            )
        }

        Divider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step 1: Name
            Section(title = stringResource(R.string.name_your_drill)) {
                TextInputField(
                    value = name,
                    placeholder = stringResource(R.string.drill_name_placeholder),
                    onValueChange = { name = it }
                )
            }

            // Step 2: Metric - vertical list
            Section(title = stringResource(R.string.what_to_focus)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricRow(
                        title = stringResource(R.string.balance_option),
                        subtitle = stringResource(R.string.balance_desc),
                        isSelected = metric == "BALANCE",
                        onClick = {
                            metric = "BALANCE"
                            targetMin = getDefaultMin(metric)
                            targetMax = getDefaultMax(metric)
                        }
                    )
                    MetricRow(
                        title = stringResource(R.string.torque_eff_short),
                        subtitle = stringResource(R.string.te_desc),
                        isSelected = metric == "TE",
                        onClick = {
                            metric = "TE"
                            targetMin = getDefaultMin(metric)
                            targetMax = getDefaultMax(metric)
                        }
                    )
                    MetricRow(
                        title = stringResource(R.string.smoothness_short),
                        subtitle = stringResource(R.string.ps_desc),
                        isSelected = metric == "PS",
                        onClick = {
                            metric = "PS"
                            targetMin = getDefaultMin(metric)
                            targetMax = getDefaultMax(metric)
                        }
                    )
                }
            }

            // Step 3: Duration - 2x2 grid + custom picker
            Section(title = stringResource(R.string.how_long)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 2x2 preset grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DurationPreset("30s", 0, 30, durationMinutes, durationSeconds, Modifier.weight(1f)) {
                            durationMinutes = 0; durationSeconds = 30
                        }
                        DurationPreset("1m", 1, 0, durationMinutes, durationSeconds, Modifier.weight(1f)) {
                            durationMinutes = 1; durationSeconds = 0
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DurationPreset("2m", 2, 0, durationMinutes, durationSeconds, Modifier.weight(1f)) {
                            durationMinutes = 2; durationSeconds = 0
                        }
                        DurationPreset("5m", 5, 0, durationMinutes, durationSeconds, Modifier.weight(1f)) {
                            durationMinutes = 5; durationSeconds = 0
                        }
                    }

                    // Custom duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NumberPicker(
                            label = stringResource(R.string.minutes),
                            value = durationMinutes,
                            range = 0..10,
                            onValueChange = { durationMinutes = it },
                            modifier = Modifier.weight(1f)
                        )
                        NumberPicker(
                            label = stringResource(R.string.seconds),
                            value = durationSeconds,
                            range = 0..59,
                            step = 5,
                            onValueChange = { durationSeconds = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (totalSeconds < 10) {
                        Text(
                            text = stringResource(R.string.min_10_seconds),
                            color = Theme.colors.problem,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Step 4: Target - vertical list
            Section(title = stringResource(R.string.set_your_target)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TargetTypeRow(
                        title = stringResource(R.string.target_min_title),
                        isSelected = targetType == "MIN",
                        onClick = { targetType = "MIN" }
                    )
                    TargetTypeRow(
                        title = stringResource(R.string.target_max_title),
                        isSelected = targetType == "MAX",
                        onClick = { targetType = "MAX" }
                    )
                    TargetTypeRow(
                        title = stringResource(R.string.target_range_title),
                        isSelected = targetType == "RANGE",
                        onClick = { targetType = "RANGE" }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Target value picker(s)
                    when (targetType) {
                        "MIN" -> {
                            TargetValuePicker(
                                label = stringResource(R.string.minimum),
                                value = targetMin ?: 50f,
                                metric = metric,
                                onValueChange = { targetMin = it }
                            )
                        }
                        "MAX" -> {
                            TargetValuePicker(
                                label = stringResource(R.string.maximum),
                                value = targetMax ?: 50f,
                                metric = metric,
                                onValueChange = { targetMax = it }
                            )
                        }
                        "RANGE" -> {
                            TargetValuePicker(
                                label = stringResource(R.string.minimum),
                                value = targetMin ?: 48f,
                                metric = metric,
                                onValueChange = { targetMin = it }
                            )
                            TargetValuePicker(
                                label = stringResource(R.string.maximum),
                                value = targetMax ?: 52f,
                                metric = metric,
                                onValueChange = { targetMax = it }
                            )
                        }
                    }
                }
            }

            // Preview
            if (name.isNotBlank()) {
                PreviewCard(
                    name = name,
                    metric = metric,
                    duration = totalSeconds,
                    targetType = targetType,
                    targetMin = targetMin,
                    targetMax = targetMax
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ==================== Layout Components ====================

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
private fun Section(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            color = Theme.colors.dim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun TextInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Theme.colors.muted,
                fontSize = 14.sp
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Theme.colors.text,
                fontSize = 14.sp
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== Metric Selection ====================

@Composable
private fun MetricRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Theme.colors.optimal.copy(alpha = 0.15f)
                else Theme.colors.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) Theme.colors.optimal else Theme.colors.text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = Theme.colors.dim,
                fontSize = 10.sp
            )
        }

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Theme.colors.optimal)
            )
        }
    }
}

// ==================== Duration Selection ====================

@Composable
private fun DurationPreset(
    label: String,
    targetMinutes: Int,
    targetSeconds: Int,
    currentMinutes: Int,
    currentSeconds: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSelected = currentMinutes == targetMinutes && currentSeconds == targetSeconds
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Theme.colors.attention.copy(alpha = 0.2f)
                else Theme.colors.surface
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Theme.colors.attention else Theme.colors.text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun NumberPicker(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Minus button
        Text(
            text = "−",
            color = if (value > range.first) Theme.colors.text else Theme.colors.muted,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(enabled = value > range.first) {
                    onValueChange((value - step).coerceIn(range))
                }
                .padding(horizontal = 8.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%02d".format(value),
                color = Theme.colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = Theme.colors.dim,
                fontSize = 9.sp
            )
        }

        // Plus button
        Text(
            text = "+",
            color = if (value < range.last) Theme.colors.text else Theme.colors.muted,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(enabled = value < range.last) {
                    onValueChange((value + step).coerceIn(range))
                }
                .padding(horizontal = 8.dp)
        )
    }
}

// ==================== Target Selection ====================

@Composable
private fun TargetTypeRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Theme.colors.attention.copy(alpha = 0.15f)
                else Theme.colors.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (isSelected) Theme.colors.attention else Theme.colors.text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Theme.colors.attention)
            )
        }
    }
}

@Composable
private fun TargetValuePicker(
    label: String,
    value: Float,
    metric: String,
    onValueChange: (Float) -> Unit
) {
    val range = when (metric) {
        "BALANCE" -> 40f..60f
        "TE" -> 50f..90f
        "PS" -> 10f..40f
        else -> 0f..100f
    }
    val step = if (metric == "BALANCE") 1f else 5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 12.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Minus
            Text(
                text = "−",
                color = if (value > range.start) Theme.colors.text else Theme.colors.muted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(enabled = value > range.start) {
                        onValueChange((value - step).coerceIn(range))
                    }
                    .padding(horizontal = 6.dp)
            )

            Text(
                text = formatTargetValue(value, metric),
                color = Theme.colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            // Plus
            Text(
                text = "+",
                color = if (value < range.endInclusive) Theme.colors.text else Theme.colors.muted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(enabled = value < range.endInclusive) {
                        onValueChange((value + step).coerceIn(range))
                    }
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

// ==================== Preview ====================

@Composable
private fun PreviewCard(
    name: String,
    metric: String,
    duration: Int,
    targetType: String,
    targetMin: Float?,
    targetMax: Float?
) {
    val metricName = when (metric) {
        "BALANCE" -> stringResource(R.string.balance_option)
        "TE" -> stringResource(R.string.torque_eff_short)
        "PS" -> stringResource(R.string.smoothness_short)
        else -> metric
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Theme.colors.optimal.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(R.string.preview).uppercase(),
            color = Theme.colors.optimal,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = name,
            color = Theme.colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PreviewStat(
                label = stringResource(R.string.metric),
                value = metricName
            )
            PreviewStat(
                label = stringResource(R.string.duration),
                value = formatDuration(duration)
            )
            PreviewStat(
                label = stringResource(R.string.target),
                value = formatTargetPreview(metric, targetType, targetMin, targetMax),
                valueColor = Theme.colors.optimal
            )
        }
    }
}

@Composable
private fun PreviewStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = Theme.colors.text
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 9.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== Helpers ====================

private fun formatTargetValue(value: Float, metric: String): String {
    return when (metric) {
        "BALANCE" -> {
            val left = (100 - value).toInt()
            val right = value.toInt()
            "$left/$right"
        }
        else -> "${value.toInt()}%"
    }
}

private fun formatTargetPreview(
    metric: String,
    targetType: String,
    targetMin: Float?,
    targetMax: Float?
): String {
    return when (metric) {
        "BALANCE" -> {
            when (targetType) {
                "MIN" -> "R≥${targetMin?.toInt()}%"
                "MAX" -> "R≤${targetMax?.toInt()}%"
                "RANGE" -> "${targetMin?.toInt()}-${targetMax?.toInt()}%"
                else -> "50/50"
            }
        }
        else -> {
            when (targetType) {
                "MIN" -> "≥${targetMin?.toInt()}%"
                "MAX" -> "≤${targetMax?.toInt()}%"
                "RANGE" -> "${targetMin?.toInt()}-${targetMax?.toInt()}%"
                else -> "-"
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m == 0 -> "${s}s"
        s == 0 -> "${m}m"
        else -> "${m}m ${s}s"
    }
}

private fun getDefaultMin(metric: String): Float {
    return when (metric) {
        "BALANCE" -> 48f
        "TE" -> 70f
        "PS" -> 20f
        else -> 50f
    }
}

private fun getDefaultMax(metric: String): Float {
    return when (metric) {
        "BALANCE" -> 52f
        "TE" -> 80f
        "PS" -> 30f
        else -> 50f
    }
}
