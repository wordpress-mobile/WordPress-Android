package org.wordpress.android.ui.newstats

import androidx.annotation.StringRes
import org.wordpress.android.R

/**
 * The top-level tabs of New Stats, in the order they appear in the tab row. Callers that open New
 * Stats on a specific tab (currently the stats deep links) name the tab through this enum.
 */
enum class StatsTab(@StringRes val titleResId: Int) {
    TRAFFIC(R.string.stats_traffic),
    INSIGHTS(R.string.stats_insights),
    SUBSCRIBERS(R.string.subscribers);

    companion object {
        /**
         * Resolves a tab from the [name] written into the Intent extra, falling back to [TRAFFIC]
         * for anything unknown so a malformed extra can't leave the screen without a tab.
         */
        fun fromName(name: String?): StatsTab = entries.firstOrNull { it.name == name } ?: TRAFFIC
    }
}
