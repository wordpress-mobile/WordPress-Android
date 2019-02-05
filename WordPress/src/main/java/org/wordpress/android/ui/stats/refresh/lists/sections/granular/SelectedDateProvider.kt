package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedDateProvider
@Inject constructor() {
    private val mutableDates = mutableMapOf<StatsGranularity, Date?>()

    private val mutableSelectedDateChanged = MutableLiveData<StatsGranularity>()
    val selectedDateChanged: LiveData<StatsGranularity> = mutableSelectedDateChanged

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        if (mutableDates[statsGranularity] != date) {
            mutableDates[statsGranularity] = date
            mutableSelectedDateChanged.value = statsGranularity
        }
    }

    fun getSelectedDate(statsGranularity: StatsGranularity) = mutableDates[statsGranularity]
    fun getCurrentDate() = Date()
    fun clear() {
        mutableDates.clear()
    }
}
