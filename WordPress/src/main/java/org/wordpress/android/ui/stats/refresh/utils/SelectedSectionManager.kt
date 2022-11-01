package org.wordpress.android.ui.stats.refresh.utils

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.ANNUAL_STATS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.TOTAL_COMMENTS_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.TOTAL_FOLLOWERS_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.TOTAL_LIKES_DETAIL
import javax.inject.Inject

const val SELECTED_SECTION_KEY = "SELECTED_STATS_SECTION_KEY"

class SelectedSectionManager
@Inject constructor(private val sharedPrefs: SharedPreferences) {
    private val _liveSelectedSection = MutableLiveData<StatsSection>()
    val liveSelectedSection: LiveData<StatsSection>
        get() {
            if (_liveSelectedSection.value == null) {
                val selectedSection = getSelectedSection()
                _liveSelectedSection.value = selectedSection
            }
            return _liveSelectedSection
        }

    fun getSelectedSection(): StatsSection {
        val value = sharedPrefs.getString(SELECTED_SECTION_KEY, INSIGHTS.name)
        return value?.let { StatsSection.valueOf(value) } ?: INSIGHTS
    }

    fun setSelectedSection(selectedSection: StatsSection) {
        sharedPrefs.edit().putString(SELECTED_SECTION_KEY, selectedSection.name).apply()
        if (this.liveSelectedSection.value != selectedSection) {
            _liveSelectedSection.postValue(selectedSection)
        }
    }
}

fun StatsSection.toStatsGranularity(): StatsGranularity? {
    return when (this) {
        ANNUAL_STATS, DETAIL, TOTAL_LIKES_DETAIL, TOTAL_COMMENTS_DETAIL, TOTAL_FOLLOWERS_DETAIL, INSIGHTS -> null
        StatsSection.INSIGHT_DETAIL,
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
