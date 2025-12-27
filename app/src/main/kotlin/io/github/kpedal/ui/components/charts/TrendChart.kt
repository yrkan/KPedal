package io.github.kpedal.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import io.github.kpedal.R
import io.github.kpedal.ui.theme.Theme

/**
 * Full trend chart with axes, labels and grid lines.
 */
@Composable
fun TrendChart(
    data: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Theme.colors.optimal,
    title: String? = null,
    unit: String = "%",
    minValue: Float = 0f,
    maxValue: Float = 100f,
    referenceValue: Float? = null
) {
    Column(modifier = modifier) {
        // Title
        title?.let {
            Text(
                text = it,
                color = Theme.colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_data_lower),
                    color = Theme.colors.dim,
                    fontSize = 11.sp
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier.width(28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${maxValue.toInt()}$unit",
                    color = Theme.colors.dim,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                referenceValue?.let {
                    Text(
                        text = "${it.toInt()}$unit",
                        color = Theme.colors.dim,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    text = "${minValue.toInt()}$unit",
                    color = Theme.colors.dim,
                    fontSize = 9.sp
                )
            }

            // Chart area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                val gridColor = Theme.colors.divider
                val refLineColor = Theme.colors.attention.copy(alpha = 0.5f)
                TrendChartCanvas(
                    data = data.map { it.value },
                    lineColor = lineColor,
                    gridColor = gridColor,
                    refLineColor = refLineColor,
                    minValue = minValue,
                    maxValue = maxValue,
                    referenceValue = referenceValue,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // X-axis labels (dates)
        if (data.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = data.first().label,
                    color = Theme.colors.dim,
                    fontSize = 9.sp
                )
                if (data.size > 1) {
                    Text(
                        text = data.last().label,
                        color = Theme.colors.dim,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendChartCanvas(
    data: List<Float>,
    lineColor: Color,
    gridColor: Color,
    refLineColor: Color,
    minValue: Float,
    maxValue: Float,
    referenceValue: Float?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (width <= 0 || height <= 0) return@Canvas

        val range = (maxValue - minValue).coerceAtLeast(1f)

        // Draw horizontal grid lines
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
            val y = height * (1 - fraction)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw reference line if provided
        referenceValue?.let { ref ->
            val refY = height - ((ref - minValue) / range * height)
            drawLine(
                color = refLineColor,
                start = Offset(0f, refY),
                end = Offset(width, refY),
                strokeWidth = 2f
            )
        }

        if (data.size < 2) {
            // Draw single point
            if (data.isNotEmpty()) {
                val y = height - ((data[0] - minValue) / range * height)
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = Offset(width / 2, y.coerceIn(0f, height))
                )
            }
            return@Canvas
        }

        // Calculate points
        val stepX = width / (data.size - 1)
        val points = data.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minValue) / range * height)
            Offset(x, y.coerceIn(0f, height))
        }

        // Draw filled area under line
        val areaPath = Path().apply {
            moveTo(0f, height)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = areaPath,
            color = lineColor.copy(alpha = 0.1f)
        )

        // Draw line
        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else {
                    lineTo(point.x, point.y)
                }
            }
        }

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw dots at data points
        points.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 3f,
                center = point
            )
        }
    }
}

/**
 * Data point for trend chart.
 */
data class TrendPoint(
    val value: Float,
    val label: String
)
