package io.github.kpedal.datatypes

import io.hammerhead.karooext.models.ViewConfig

/**
 * Layout utilities for DataTypes.
 *
 * Provides layout size calculation and constants used by all Glance DataTypes.
 * Originally an abstract class for RemoteViews-based DataTypes, now simplified
 * to just the static utilities needed by Glance.
 */
object BaseDataType {

    /**
     * Text to display when no data is available.
     * Standard cycling computer convention.
     */
    const val NO_DATA = "-"

    /**
     * Text to display when sensor is disconnected.
     * Two dashes to differentiate from single dash (no data yet).
     */
    const val SENSOR_DISCONNECTED = "--"

    /**
     * Size category for adaptive layouts.
     * Based on gridSize (60 units total) and viewSize (pixels).
     *
     * Karoo 3 screen: 480x800px, grid: 60x60 units
     *
     * Full width (gridSize.first >= 50):
     * - 1 field:     (60,60) 470x790 → LARGE
     * - 2 fields:    (60,30) 470x390 → LARGE
     * - 3 fields:    (60,20) 470x260 → MEDIUM
     * - 4 fields:    (60,15) 470x195 → MEDIUM_WIDE (2-row wide)
     * - 5 fields 2:1:(60,24) 470x320 → MEDIUM (large fields)
     * - 5 fields 2:1:(60,12) 470x160 → SMALL_WIDE (small fields)
     * - 6 fields:    (60,10) 470x130 → SMALL_WIDE (1-row wide)
     *
     * Half width (gridSize.first < 50):
     * - 2 horiz:  (30,60) 235x790 → NARROW (tall, narrow)
     * - 2x2 grid: (30,30) 235x390 → MEDIUM
     * - 2x3 grid: (30,20) 235x260 → MEDIUM
     * - 2x4 grid: (30,15) 235x195 → SMALL
     * - 2x5 grid: (30,12) 235x156 → SMALL
     * - 2x6 grid: (30,10) 235x130 → SMALL
     */
    enum class LayoutSize {
        SMALL,       // Short height, narrow width - minimal content
        SMALL_WIDE,  // 1-row wide, full width, very short (~130-160px) - horizontal compact
        MEDIUM_WIDE, // 2-row wide, full width, short (~160-220px) - more space than SMALL_WIDE
        MEDIUM,      // 3-row wide or half width medium - balanced layout (~220-350px)
        LARGE,       // Large height, full width - detailed layout (350px+)
        NARROW       // Tall height, narrow width (2 fields side by side)
    }

    /**
     * Determine layout size category from ViewConfig.
     * Uses gridSize.first to detect full width (60) vs half width (30).
     *
     * Karoo 3 screen: 480x800px, grid: 60x60 units
     *
     * Full width (gridSize.first >= 50):
     * - 1 field:     (60,60) 470x790 → LARGE
     * - 2 fields:    (60,30) 470x390 → LARGE
     * - 3 fields:    (60,20) 470x260 → MEDIUM
     * - 4 fields:    (60,15) 470x195 → MEDIUM_WIDE
     * - 5 fields 2:1:(60,24) 470x320 → MEDIUM (large fields)
     * - 5 fields 2:1:(60,12) 470x160 → SMALL_WIDE (small fields)
     * - 6 fields:    (60,10) 470x130 → SMALL_WIDE
     *
     * Half width (gridSize.first < 50):
     * - 2 horiz:  (30,60) 235x790 → NARROW (tall, narrow)
     * - 2x2 grid: (30,30) 235x390 → MEDIUM
     * - 2x3 grid: (30,20) 235x260 → MEDIUM
     * - 2x4 grid: (30,15) 235x195 → SMALL
     * - 2x5 grid: (30,12) 235x156 → SMALL
     * - 2x6 grid: (30,10) 235x130 → SMALL
     */
    fun getLayoutSize(config: ViewConfig): LayoutSize {
        val isFullWidth = config.gridSize.first >= 50  // 60 = full, 30 = half
        val height = config.viewSize.second

        return if (isFullWidth) {
            // Full width layouts
            when {
                height >= 250 -> LayoutSize.LARGE       // 1-3 fields stacked
                height >= 160 -> LayoutSize.MEDIUM_WIDE // 4 fields (2-row wide)
                else -> LayoutSize.SMALL_WIDE           // 5-6 fields (1-row wide)
            }
        } else {
            // Half width layouts (side by side)
            when {
                height >= 600 -> LayoutSize.NARROW      // 2 fields side by side (tall, narrow)
                height >= 200 -> LayoutSize.MEDIUM      // 2x2, 2x3 grid
                else -> LayoutSize.SMALL                // 2x4+ grid cells (minimal content)
            }
        }
    }
}
