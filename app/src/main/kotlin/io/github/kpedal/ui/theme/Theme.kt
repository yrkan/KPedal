package io.github.kpedal.ui.theme

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import android.view.View

/**
 * kpedal color palette
 * Principle: Color = Status
 * White = normal, Colored = needs attention
 */
object KPedalColors {
    // Background
    val background = Color(0xFF000000)
    val surface = Color(0xFF111111)
    val surfaceVariant = Color(0xFF1A1A1A)

    // Text
    val text = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFAAAAAA)
    val dim = Color(0xFF666666)
    val muted = Color(0xFF333333)

    // Status colors
    val optimal = Color(0xFF4CAF50)    // Green
    val attention = Color(0xFFFFC107)  // Yellow/Amber
    val problem = Color(0xFFF44336)    // Red

    // Dividers
    val divider = Color(0xFF222222)

    // Primary (uses optimal green)
    val primary = optimal
    val onPrimary = Color(0xFF000000)
}

@Immutable
data class KPedalColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val text: Color,
    val textSecondary: Color,
    val dim: Color,
    val muted: Color,
    val optimal: Color,
    val attention: Color,
    val problem: Color,
    val divider: Color,
    val primary: Color,
    val onPrimary: Color
)

val LocalKPedalColors = staticCompositionLocalOf {
    KPedalColorScheme(
        background = KPedalColors.background,
        surface = KPedalColors.surface,
        surfaceVariant = KPedalColors.surfaceVariant,
        text = KPedalColors.text,
        textSecondary = KPedalColors.textSecondary,
        dim = KPedalColors.dim,
        muted = KPedalColors.muted,
        optimal = KPedalColors.optimal,
        attention = KPedalColors.attention,
        problem = KPedalColors.problem,
        divider = KPedalColors.divider,
        primary = KPedalColors.primary,
        onPrimary = KPedalColors.onPrimary
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = KPedalColors.primary,
    onPrimary = KPedalColors.onPrimary,
    secondary = KPedalColors.optimal,
    background = KPedalColors.background,
    surface = KPedalColors.surface,
    onBackground = KPedalColors.text,
    onSurface = KPedalColors.text,
)

@Composable
fun KPedalTheme(
    content: @Composable () -> Unit
) {
    val colors = KPedalColorScheme(
        background = KPedalColors.background,
        surface = KPedalColors.surface,
        surfaceVariant = KPedalColors.surfaceVariant,
        text = KPedalColors.text,
        textSecondary = KPedalColors.textSecondary,
        dim = KPedalColors.dim,
        muted = KPedalColors.muted,
        optimal = KPedalColors.optimal,
        attention = KPedalColors.attention,
        problem = KPedalColors.problem,
        divider = KPedalColors.divider,
        primary = KPedalColors.primary,
        onPrimary = KPedalColors.onPrimary
    )

    val configuration = LocalConfiguration.current
    val layoutDirection = if (configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalKPedalColors provides colors
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}

// Convenience accessors
object Theme {
    val colors: KPedalColorScheme
        @Composable get() = LocalKPedalColors.current
}
