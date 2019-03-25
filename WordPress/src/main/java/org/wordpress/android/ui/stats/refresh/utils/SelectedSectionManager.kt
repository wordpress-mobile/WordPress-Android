package org.wordpress.android.ui.stats.refresh.utils

import android.content.SharedPreferences
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import javax.inject.Inject

const val SELECTED_SECTION_KEY = "SELECTED_STATS_SECTION_KEY"

class SelectedSectionManager
@Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun getSelectedSection(): StatsSection {
        val value = sharedPrefs.getString(SELECTED_SECTION_KEY, StatsSection.INSIGHTS.name)
        return value?.let { StatsSection.valueOf(value) } ?: StatsSection.INSIGHTS
    }

    fun getSelectedStatsGranularity(): StatsGranularity? {
        return getSelectedSection().toStatsGranularity()
    }

    fun setSelectedSection(selectedSection: StatsSection) {
        sharedPrefs.edit().putString(SELECTED_SECTION_KEY, selectedSection.name).apply()
    }
}

fun StatsSection.toStatsGranularity(): StatsGranularity? {
    return when (this) {
        DETAIL, INSIGHTS -> null
        StatsSection.DAYS -> DAYS
        StatsSection.WEEKS -> WEEKS
        StatsSection.MONTHS -> MONTHS
        StatsSection.YEARS -> YEARS
    }
}

fun StatsGranularity.toStatsSection(): StatsSection {
    return when (this) {
        DAYS -> StatsSection.DAYS
        WEEKS -> StatsSection.WEEKS
        MONTHS -> StatsSection.MONTHS
        YEARS -> StatsSection.YEARS
    }
}
