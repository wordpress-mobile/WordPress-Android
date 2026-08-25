package org.wordpress.android.ui.newstats

import androidx.compose.ui.graphics.Color

/**
 * Shared color constants for the Stats UI components.
 * Centralizes color definitions for consistency and maintainability.
 */
object StatsColors {
    val ChangeBadgePositive = Color(0xFF2E7D32)
    val ChangeBadgeNegative = Color(0xFFE91E63)

    // Per-metric chart accent colors, used when a metric is selected as the charted series so the
    // chart recolours per selection (matching iOS's SiteMetric.primaryColor). Views intentionally
    // has no color here: it falls back to the theme primary for visual continuity with the previous
    // views-only chart. These are sensible defaults; design may tune them later.
    val MetricVisitors = Color(0xFF7E57C2)
    val MetricLikes = Color(0xFFEF5350)
    val MetricComments = Color(0xFF26A69A)
    val MetricPosts = Color(0xFFFFA726)
}
