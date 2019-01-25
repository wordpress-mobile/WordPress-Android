package org.wordpress.android.ui.stats.refresh.utils

import android.content.SharedPreferences
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import javax.inject.Inject

const val SELECTED_SECTION_KEY = "SELECTED_STATS_SECTION_KEY"

class SelectedSectionManager
@Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun getSelectedSection(): StatsSection {
        return StatsSection.valueOf(sharedPrefs.getString(SELECTED_SECTION_KEY, StatsSection.INSIGHTS.name))
    }

    fun setSelectedSection(selectedSection: StatsSection) {
        sharedPrefs.edit().putString(SELECTED_SECTION_KEY, selectedSection.name).apply()
    }
}
