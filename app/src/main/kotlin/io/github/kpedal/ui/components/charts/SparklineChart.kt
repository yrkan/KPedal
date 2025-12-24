package io.github.kpedal.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.kpedal.ui.theme.Theme

/**
 * Simple sparkline chart for displaying trends.
 * Uses Canvas for efficient rendering without external dependencies.
 */
@Composable
fun SparklineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Theme.colors.optimal,
    strokeWidth: Float = 2f,
    showDots: Boolean = false,
    minValue: Float? = null,
    maxValue: Float? = null
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0 || height <= 0 || data.size < 2) return@Canvas

        // Calculate bounds
        val min = minValue ?: data.minOrNull() ?: 0f
        val max = maxValue ?: data.maxOrNull() ?: 100f
        val range = (max - min).coerceAtLeast(1f)

        // Calculate points
        val stepX = width / (data.size - 1)
        val points = data.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - min) / range * height)
            Offset(x, y.coerceIn(0f, height))
        }

        // Draw line path
        val path = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else {
                    lineTo(point.x, point.y)
                }
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw dots at data points
        if (showDots) {
            points.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = strokeWidth * 1.5f,
                    center = point
                )
            }
        }
    }
}

/**
 * Sparkline with a reference line (e.g., for balance showing 50% line).
 */
@Composable
fun SparklineWithReference(
    data: List<Float>,
    referenceValue: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = Theme.colors.optimal,
    referenceColor: Color = Theme.colors.dim,
    strokeWidth: Float = 2f,
    minValue: Float = 0f,
    maxValue: Float = 100f
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0 || height <= 0) return@Canvas

        val range = (maxValue - minValue).coerceAtLeast(1f)

        // Draw reference line
        val refY = height - ((referenceValue - minValue) / range * height)
        drawLine(
            color = referenceColor,
            start = Offset(0f, refY),
            end = Offset(width, refY),
            strokeWidth = 1f
        )

        if (data.size < 2) return@Canvas

        // Calculate points
        val stepX = width / (data.size - 1)
        val points = data.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minValue) / range * height)
            Offset(x, y.coerceIn(0f, height))
        }

        // Draw line path
        val path = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else {
                    lineTo(point.x, point.y)
                }
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
