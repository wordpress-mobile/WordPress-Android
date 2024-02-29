package org.wordpress.android.ui.stats.refresh.utils

import android.content.SharedPreferences
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import javax.inject.Inject
import javax.inject.Singleton

const val SELECTED_TRAFFIC_GRANULARITY_KEY = "SELECTED_TRAFFIC_GRANULARITY_KEY"

@Singleton
class SelectedTrafficGranularityManager @Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun getSelectedTrafficGranularity(): StatsGranularity {
        val value = sharedPrefs.getString(SELECTED_TRAFFIC_GRANULARITY_KEY, DAYS.name)
        return value?.let { StatsGranularity.valueOf(value) } ?: DAYS
    }

    fun setSelectedTrafficGranularity(selectedTrafficGranularity: StatsGranularity) {
        sharedPrefs.edit().putString(SELECTED_TRAFFIC_GRANULARITY_KEY, selectedTrafficGranularity.name).apply()
    }
}
