package io.github.kpedal.datatypes.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.kpedal.datatypes.BaseDataType
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.engine.StatusCalculator

/**
 * Reusable Glance components for DataTypes.
 */

/**
 * Standard container for all data fields.
 * Creates a subtle frame around the content that provides visual separation
 * between adjacent cells on the Karoo grid.
 *
 * Structure:
 * - Outer: Frame color (#1A1A1A) with 1dp padding creates the "border"
 * - Inner: Black background (#000000) with 5dp padding for content
 *
 * Result: When cells are adjacent, there's a 2dp gap (1dp from each cell)
 * in a subtle dark gray that separates them visually.
 */
@Composable
fun DataFieldContainer(
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit
) {
    // Outer frame - creates the border effect
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlanceColors.Frame)
            .padding(1.dp)
    ) {
        // Inner content area
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceColors.Background)
                .padding(6.dp)
        ) {
            content()
        }
    }
}

/**
 * Horizontal divider line with subtle margins.
 * Does not extend edge-to-edge for a more refined look.
 *
 * @param modifier Additional modifier
 * @param verticalPadding Vertical breathing room above/below the line (default 4dp)
 * @param horizontalPadding Horizontal inset from edges (default 8dp)
 */
@Composable
fun GlanceDivider(
    modifier: GlanceModifier = GlanceModifier,
    verticalPadding: Int = 4,
    horizontalPadding: Int = 8
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding.dp, horizontal = horizontalPadding.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlanceColors.Divider)
        ) {}
    }
}

/**
 * Vertical divider line.
 *
 * @param modifier Glance modifier
 * @param height Divider height in dp. Should match surrounding text size:
 *               - 16dp for 14-16sp text
 *               - 20dp for 18-20sp text
 *               - 24dp for 22-24sp text
 *               - 28dp for 28-32sp text
 */
@Composable
fun GlanceVerticalDivider(
    modifier: GlanceModifier = GlanceModifier,
    height: Int = 16
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(height.dp)
            .background(GlanceColors.Divider)
    ) {}
}

/**
 * Label text (small, dim).
 *
 * @param text The label text
 * @param modifier Glance modifier
 * @param fontSize Font size in sp. Use larger sizes for bigger layouts:
 *                 - SMALL/SMALL_WIDE/MEDIUM_WIDE: 10 (default)
 *                 - MEDIUM/NARROW: 11
 *                 - LARGE: 12
 */
@Composable
fun LabelText(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
    fontSize: Int = 10
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize.sp,
            color = ColorProvider(GlanceColors.Label)
        ),
        maxLines = 1
    )
}

/**
 * Value text (large, bold, colored).
 *
 * @param text The value text
 * @param color Text color
 * @param fontSize Font size in sp
 * @param modifier Glance modifier
 */
@Composable
fun ValueText(
    text: String,
    color: Color = GlanceColors.White,
    fontSize: Int = 18,
    modifier: GlanceModifier = GlanceModifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = ColorProvider(color),
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
}

/**
 * Balance row with left and right values.
 */
@Composable
fun BalanceRow(
    leftValue: String,
    rightValue: String,
    leftColor: Color,
    rightColor: Color,
    modifier: GlanceModifier = GlanceModifier,
    valueFontSize: Int = 18
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            ValueText(leftValue, leftColor, valueFontSize)
        }
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            ValueText(rightValue, rightColor, valueFontSize)
        }
    }
}

/**
 * Metric row with label and left/right values.
 */
@Composable
fun MetricRow(
    label: String,
    leftValue: String,
    rightValue: String,
    leftColor: Color,
    rightColor: Color,
    modifier: GlanceModifier = GlanceModifier,
    valueFontSize: Int = 16,
    labelFontSize: Int = 10
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelText(label, GlanceModifier.padding(end = 4.dp), labelFontSize)
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            ValueText(leftValue, leftColor, valueFontSize)
        }
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            ValueText(rightValue, rightColor, valueFontSize)
        }
    }
}

/**
 * Get display values for metrics.
 */
fun getDisplayValue(
    metrics: PedalingMetrics,
    sensorDisconnected: Boolean,
    hasData: Boolean = metrics.hasData,
    value: () -> String
): String {
    return when {
        sensorDisconnected -> BaseDataType.SENSOR_DISCONNECTED
        !hasData -> BaseDataType.NO_DATA
        else -> value()
    }
}

/**
 * Get balance colors based on status.
 */
fun getBalanceColors(metrics: PedalingMetrics): Pair<Color, Color> {
    val left = metrics.balanceLeft.toInt()
    val right = metrics.balance.toInt()
    val status = StatusCalculator.balanceStatus(metrics.balance)

    return if (status != StatusCalculator.Status.OPTIMAL) {
        val statusColor = when (status) {
            StatusCalculator.Status.ATTENTION -> GlanceColors.Attention
            StatusCalculator.Status.PROBLEM -> GlanceColors.Problem
            else -> GlanceColors.White
        }
        if (left > 52) {
            Pair(statusColor, GlanceColors.White)
        } else {
            Pair(GlanceColors.White, statusColor)
        }
    } else {
        Pair(GlanceColors.White, GlanceColors.White)
    }
}

/**
 * Get color for TE status.
 */
fun getTEColor(te: Float): Color {
    val status = StatusCalculator.teStatus(te)
    return when (status) {
        StatusCalculator.Status.OPTIMAL -> GlanceColors.White
        StatusCalculator.Status.ATTENTION -> GlanceColors.Attention
        StatusCalculator.Status.PROBLEM -> GlanceColors.Problem
    }
}

/**
 * Get color for PS status.
 */
fun getPSColor(ps: Float): Color {
    val status = StatusCalculator.psStatus(ps)
    return when (status) {
        StatusCalculator.Status.OPTIMAL -> GlanceColors.White
        StatusCalculator.Status.ATTENTION -> GlanceColors.Attention
        StatusCalculator.Status.PROBLEM -> GlanceColors.Problem
    }
}

/**
 * Get status color for display.
 */
fun getStatusColor(status: StatusCalculator.Status): Color {
    return when (status) {
        StatusCalculator.Status.OPTIMAL -> GlanceColors.Optimal
        StatusCalculator.Status.ATTENTION -> GlanceColors.Attention
        StatusCalculator.Status.PROBLEM -> GlanceColors.Problem
    }
}

/**
 * Get zone status color based on time in zone percentages.
 * Returns the color of the dominant zone.
 */
fun getZoneStatusColor(optimal: Int, attention: Int, problem: Int): Color {
    return when {
        optimal >= attention && optimal >= problem -> GlanceColors.Optimal
        attention >= problem -> GlanceColors.Attention
        else -> GlanceColors.Problem
    }
}

/**
 * Zone status bar - single color indicating overall ride quality.
 */
@Composable
fun ZoneStatusBar(
    optimal: Int,
    attention: Int,
    problem: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    val statusColor = getZoneStatusColor(optimal, attention, problem)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(statusColor)
    ) {}
}

/**
 * Professional segmented zone bar showing proportional time in each zone.
 * Displays optimal (green), attention (yellow), and problem (red) segments.
 *
 * @param optimal Percentage in optimal zone (0-100)
 * @param attention Percentage in attention zone (0-100)
 * @param problem Percentage in problem zone (0-100)
 * @param height Bar height in dp
 * @param modifier Additional modifier
 */
@Composable
fun ZoneSegmentedBar(
    optimal: Int,
    attention: Int,
    problem: Int,
    height: Int = 8,
    modifier: GlanceModifier = GlanceModifier
) {
    // Ensure we have valid percentages
    val total = optimal + attention + problem
    val safeOptimal = if (total > 0) optimal else 33
    val safeAttention = if (total > 0) attention else 33
    val safeProblem = if (total > 0) problem else 34

    Row(
        modifier = modifier.fillMaxWidth().height(height.dp)
    ) {
        // Optimal segment (green)
        if (safeOptimal > 0) {
            Box(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .defaultWeight()
                    .background(GlanceColors.Optimal)
            ) {}
        }

        // Attention segment (yellow)
        if (safeAttention > 0) {
            Box(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .defaultWeight()
                    .background(GlanceColors.Attention)
            ) {}
        }

        // Problem segment (red)
        if (safeProblem > 0) {
            Box(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .defaultWeight()
                    .background(GlanceColors.Problem)
            ) {}
        }
    }
}

/**
 * Zone indicator chip - colored box with percentage value.
 */
@Composable
fun ZoneChip(
    value: Int,
    color: Color,
    modifier: GlanceModifier = GlanceModifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator dot
        Box(
            modifier = GlanceModifier
                .width(8.dp)
                .height(8.dp)
                .background(color)
        ) {}
        // Value
        ValueText("$value%", color, 14, GlanceModifier.padding(start = 4.dp))
    }
}
