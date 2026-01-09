package io.github.kpedal.datatypes.glance

import androidx.compose.ui.graphics.Color

/**
 * Color definitions for Glance-based DataTypes.
 * Matches StatusCalculator colors but in Compose Color format.
 *
 * Colors optimized for outdoor readability on Karoo display:
 * - High contrast on black background
 * - Visible in bright sunlight
 */
object GlanceColors {
    val White = Color(0xFFFFFFFF)
    val Optimal = Color(0xFF4CAF50)     // Green - good visibility
    val Attention = Color(0xFFFF9800)   // Orange - better sunlight visibility
    val Problem = Color(0xFFF44336)     // Red - good visibility
    val Label = Color(0xFFAAAAAA)       // Light gray - clear label readability
    val Separator = Color(0xFF666666)   // Medium gray - for | and / separators
    val Divider = Color(0xFF555555)     // Divider lines - visible section separators
    val Background = Color(0xFF000000)  // Black background - content area
    val Frame = Color(0xFF1A1A1A)       // Very dark gray - creates subtle border between cells

    @Deprecated("Use Label instead", ReplaceWith("Label"))
    val Dim = Label
}
