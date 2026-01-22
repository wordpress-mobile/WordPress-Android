package org.wordpress.android.ui.newstats.util

import java.util.Locale

private const val THOUSAND = 1_000
private const val MILLION = 1_000_000
private const val FORMAT_MILLION = "%.1fM"
private const val FORMAT_THOUSAND = "%.1fK"

/**
 * Formats a stat value for display, using K/M suffixes for large numbers.
 * Examples: 1500 -> "1.5K", 2500000 -> "2.5M", 500 -> "500"
 */
fun formatStatValue(value: Long): String {
    return when {
        value >= MILLION -> String.format(Locale.getDefault(), FORMAT_MILLION, value / MILLION.toDouble())
        value >= THOUSAND -> String.format(Locale.getDefault(), FORMAT_THOUSAND, value / THOUSAND.toDouble())
        else -> value.toString()
    }
}
