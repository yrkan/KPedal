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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.data.database.CustomDrillEntity
import io.github.kpedal.ui.theme.Theme

/**
 * Screen for creating or editing a custom drill.
 * Step-by-step flow for clarity.
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
    var durationMinutes by remember { mutableStateOf((existingDrill?.durationSeconds ?: 30) / 60) }
    var durationSeconds by remember { mutableStateOf((existingDrill?.durationSeconds ?: 30) % 60) }
    var targetType by remember { mutableStateOf(existingDrill?.targetType ?: "RANGE") }
    var targetMin by remember { mutableStateOf(existingDrill?.targetMin ?: getDefaultMin(metric)) }
    var targetMax by remember { mutableStateOf(existingDrill?.targetMax ?: getDefaultMax(metric)) }

    val totalSeconds = durationMinutes * 60 + durationSeconds
    val canSave = name.isNotBlank() && totalSeconds >= 10

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
                text = "Cancel",
                color = Theme.colors.dim,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() }
            )
            Text(
                text = if (isEdit) "Edit Drill" else "New Drill",
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Save",
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
                            description = "Custom ${getMetricName(metric)} drill",
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
                .padding(12.dp)
        ) {
            // Step 1: Name
            StepHeader(step = 1, title = "Name your drill")
            Spacer(modifier = Modifier.height(8.dp))
            TextInputField(
                value = name,
                placeholder = "e.g. Balance Focus, High TE",
                onValueChange = { name = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Step 2: Metric
            StepHeader(step = 2, title = "What to focus on?")
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricOption(
                    title = "Balance",
                    description = "L/R power distribution",
                    isSelected = metric == "BALANCE",
                    onClick = {
                        metric = "BALANCE"
                        targetMin = getDefaultMin(metric)
                        targetMax = getDefaultMax(metric)
                    }
                )
                MetricOption(
                    title = "Torque Effectiveness",
                    description = "Power transfer efficiency",
                    isSelected = metric == "TE",
                    onClick = {
                        metric = "TE"
                        targetMin = getDefaultMin(metric)
                        targetMax = getDefaultMax(metric)
                    }
                )
                MetricOption(
                    title = "Pedal Smoothness",
                    description = "Circular pedal stroke",
                    isSelected = metric == "PS",
                    onClick = {
                        metric = "PS"
                        targetMin = getDefaultMin(metric)
                        targetMax = getDefaultMax(metric)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 3: Duration
            StepHeader(step = 3, title = "How long?")
            Spacer(modifier = Modifier.height(8.dp))

            // Quick presets
            Text(
                text = "Quick select",
                color = Theme.colors.dim,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDurationChip("30s", 0, 30, durationMinutes, durationSeconds) {
                    durationMinutes = 0; durationSeconds = 30
                }
                QuickDurationChip("1m", 1, 0, durationMinutes, durationSeconds) {
                    durationMinutes = 1; durationSeconds = 0
                }
                QuickDurationChip("2m", 2, 0, durationMinutes, durationSeconds) {
                    durationMinutes = 2; durationSeconds = 0
                }
                QuickDurationChip("3m", 3, 0, durationMinutes, durationSeconds) {
                    durationMinutes = 3; durationSeconds = 0
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom duration
            Text(
                text = "Or set custom",
                color = Theme.colors.dim,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DurationPicker(
                    label = "Minutes",
                    value = durationMinutes,
                    range = 0..10,
                    onValueChange = { durationMinutes = it },
                    modifier = Modifier.weight(1f)
                )
                DurationPicker(
                    label = "Seconds",
                    value = durationSeconds,
                    range = 0..59,
                    step = 5,
                    onValueChange = { durationSeconds = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // Duration display
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: ${formatDuration(totalSeconds)}",
                color = if (totalSeconds >= 10) Theme.colors.text else Theme.colors.problem,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (totalSeconds < 10) {
                Text(
                    text = "Minimum 10 seconds",
                    color = Theme.colors.problem,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 4: Target
            StepHeader(step = 4, title = "Set your target")
            Spacer(modifier = Modifier.height(8.dp))

            // Target type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetTypeOption(
                    title = "Above",
                    description = "≥ value",
                    isSelected = targetType == "MIN",
                    onClick = { targetType = "MIN" },
                    modifier = Modifier.weight(1f)
                )
                TargetTypeOption(
                    title = "Below",
                    description = "≤ value",
                    isSelected = targetType == "MAX",
                    onClick = { targetType = "MAX" },
                    modifier = Modifier.weight(1f)
                )
                TargetTypeOption(
                    title = "Range",
                    description = "min-max",
                    isSelected = targetType == "RANGE",
                    onClick = { targetType = "RANGE" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Target values
            when (targetType) {
                "MIN" -> {
                    TargetValueRow(
                        label = "Minimum",
                        value = targetMin ?: 50f,
                        metric = metric,
                        onValueChange = { targetMin = it }
                    )
                }
                "MAX" -> {
                    TargetValueRow(
                        label = "Maximum",
                        value = targetMax ?: 50f,
                        metric = metric,
                        onValueChange = { targetMax = it }
                    )
                }
                "RANGE" -> {
                    TargetValueRow(
                        label = "Minimum",
                        value = targetMin ?: 48f,
                        metric = metric,
                        onValueChange = { targetMin = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TargetValueRow(
                        label = "Maximum",
                        value = targetMax ?: 52f,
                        metric = metric,
                        onValueChange = { targetMax = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preview card
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
private fun StepHeader(step: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Theme.colors.optimal.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$step",
                color = Theme.colors.optimal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Theme.colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
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
            .padding(14.dp)
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

@Composable
private fun MetricOption(
    title: String,
    description: String,
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                color = if (isSelected) Theme.colors.optimal else Theme.colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Theme.colors.dim,
                fontSize = 10.sp
            )
        }
        if (isSelected) {
            Text(
                text = "✓",
                color = Theme.colors.optimal,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickDurationChip(
    label: String,
    targetMinutes: Int,
    targetSeconds: Int,
    currentMinutes: Int,
    currentSeconds: Int,
    onClick: () -> Unit
) {
    val isSelected = currentMinutes == targetMinutes && currentSeconds == targetSeconds
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) Theme.colors.attention.copy(alpha = 0.2f)
                else Theme.colors.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Theme.colors.attention else Theme.colors.text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun DurationPicker(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Theme.colors.dim,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
            Text(
                text = "%02d".format(value),
                color = Theme.colors.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
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
}

@Composable
private fun TargetTypeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Theme.colors.attention.copy(alpha = 0.15f)
                else Theme.colors.surface
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (isSelected) Theme.colors.attention else Theme.colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            color = Theme.colors.dim,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun TargetValueRow(
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

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

@Composable
private fun PreviewCard(
    name: String,
    metric: String,
    duration: Int,
    targetType: String,
    targetMin: Float?,
    targetMax: Float?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Theme.colors.surface)
            .padding(14.dp)
    ) {
        Text(
            text = "PREVIEW",
            color = Theme.colors.dim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            color = Theme.colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Metric", color = Theme.colors.muted, fontSize = 9.sp)
                Text(
                    text = getMetricName(metric),
                    color = Theme.colors.text,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Duration", color = Theme.colors.muted, fontSize = 9.sp)
                Text(
                    text = formatDuration(duration),
                    color = Theme.colors.text,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Target", color = Theme.colors.muted, fontSize = 9.sp)
                Text(
                    text = formatTargetPreview(metric, targetType, targetMin, targetMax),
                    color = Theme.colors.optimal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

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

private fun getMetricName(metric: String): String {
    return when (metric) {
        "BALANCE" -> "Balance"
        "TE" -> "Torque Eff."
        "PS" -> "Smoothness"
        else -> metric
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
