package org.wordpress.android.ui.newstats

import androidx.compose.ui.graphics.Color

/**
 * Shared color constants for the Stats UI components.
 * Centralizes color definitions for consistency and maintainability.
 */
object StatsColors {
    val ChangeBadgePositive = Color(0xFF2E7D32)
    val ChangeBadgeNegative = Color(0xFFE91E63)

    /**
     * The selected bar in a day-views chart. Shares the negative badge's hue, but it marks the
     * user's selection, not a decline -- keep them separate so either can move independently.
     */
    val ChartSelectedBar = Color(0xFFE91E63)
}
