package io.github.kpedal.datatypes

import android.util.TypedValue
import android.widget.RemoteViews
import io.hammerhead.karooext.models.ViewConfig

/**
 * Calculates adaptive text sizes based on ViewConfig from Karoo SDK.
 *
 * Uses config.textSize (SDK-recommended font size) as base and applies
 * multipliers based on the semantic role of the text element.
 *
 * This allows data fields to scale properly for any size field,
 * from tiny 6x10 fields to full-screen displays.
 */
object TextSizeCalculator {

    /**
     * Semantic role of text element, determines size multiplier.
     */
    enum class Role {
        /** Main values (balance L/R, power, etc.) - largest text */
        PRIMARY,
        /** Secondary values (TE/PS L/R when balance is primary) */
        SECONDARY,
        /** Tertiary values (smoothed balance, averages) */
        TERTIARY,
        /** Labels and descriptions ("Balance", "Left", "Right") */
        LABEL,
        /** Status icons (checkmark, warning) - larger than primary */
        ICON
    }

    /**
     * Multipliers relative to config.textSize for each role.
     * These values are tuned for Karoo display (480x800px).
     */
    private val MULTIPLIERS = mapOf(
        Role.PRIMARY to 1.0f,
        Role.SECONDARY to 0.72f,
        Role.TERTIARY to 0.58f,
        Role.LABEL to 0.38f,
        Role.ICON to 1.4f
    )

    /**
     * Minimum text sizes in pixels to ensure readability.
     */
    private val MIN_SIZES = mapOf(
        Role.PRIMARY to 16f,
        Role.SECONDARY to 14f,
        Role.TERTIARY to 12f,
        Role.LABEL to 10f,
        Role.ICON to 20f
    )

    /**
     * Maximum text sizes in pixels to prevent overflow.
     */
    private val MAX_SIZES = mapOf(
        Role.PRIMARY to 80f,
        Role.SECONDARY to 56f,
        Role.TERTIARY to 44f,
        Role.LABEL to 16f,
        Role.ICON to 100f
    )

    /**
     * Calculate text size in pixels for a given role.
     *
     * @param config ViewConfig from Karoo SDK containing textSize
     * @param role Semantic role of the text element
     * @return Text size in pixels, clamped to min/max bounds
     */
    fun calculate(config: ViewConfig, role: Role): Float {
        val baseSize = config.textSize.toFloat()
        val multiplier = MULTIPLIERS[role] ?: 1.0f
        val minSize = MIN_SIZES[role] ?: 10f
        val maxSize = MAX_SIZES[role] ?: 100f

        return (baseSize * multiplier).coerceIn(minSize, maxSize)
    }

    /**
     * Calculate progress bar height based on view size.
     * Scales proportionally to view height.
     *
     * @param config ViewConfig containing viewSize
     * @return Height in pixels, clamped to 4-16dp equivalent
     */
    fun calculateBarHeight(config: ViewConfig): Int {
        val viewHeight = config.viewSize.second
        // Bar height = 3-4% of view height, clamped to reasonable bounds
        return (viewHeight * 0.035f).toInt().coerceIn(4, 16)
    }

    /**
     * Calculate padding based on view size.
     * Smaller views need less padding to maximize content area.
     *
     * @param config ViewConfig containing viewSize
     * @return Padding in pixels
     */
    fun calculatePadding(config: ViewConfig): Int {
        val viewHeight = config.viewSize.second
        return when {
            viewHeight < 80 -> 2
            viewHeight < 120 -> 4
            viewHeight < 200 -> 6
            else -> 8
        }
    }

    /**
     * Calculate divider height based on view size.
     *
     * @param config ViewConfig containing viewSize
     * @return Divider height in pixels (1-2)
     */
    fun calculateDividerHeight(config: ViewConfig): Int {
        return if (config.viewSize.second < 150) 1 else 2
    }
}

/**
 * Extension function to set text size in pixels on a RemoteViews TextView.
 *
 * @param viewId Resource ID of the TextView
 * @param sizePx Text size in pixels
 */
fun RemoteViews.setTextSizePx(viewId: Int, sizePx: Float) {
    setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_PX, sizePx)
}

/**
 * Extension function to set text size using TextSizeCalculator.
 *
 * @param viewId Resource ID of the TextView
 * @param config ViewConfig from Karoo SDK
 * @param role Semantic role for size calculation
 */
fun RemoteViews.setAdaptiveTextSize(viewId: Int, config: ViewConfig, role: TextSizeCalculator.Role) {
    val size = TextSizeCalculator.calculate(config, role)
    setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_PX, size)
}
