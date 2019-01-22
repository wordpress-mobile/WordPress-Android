package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedDateProvider
@Inject constructor() {
    private val mutableDates = mutableMapOf(
            DAYS to SelectedDate(loading = true),
            WEEKS to SelectedDate(loading = true),
            MONTHS to SelectedDate(loading = true),
            YEARS to SelectedDate(loading = true)
    )

    private val mutableSelectedDateChanged = MutableLiveData<StatsGranularity>()
    val selectedDateChanged: LiveData<StatsGranularity> = mutableSelectedDateChanged

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDate(statsGranularity)
        if (selectedDate.date != date) {
            mutableDates[statsGranularity] = SelectedDate(date)
            mutableSelectedDateChanged.value = statsGranularity
        }
    }

    fun dateLoadingFailed(statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDate(statsGranularity)
        if (selectedDate.date != null && !selectedDate.error) {
            mutableDates[statsGranularity] = selectedDate.copy(error = true)
            mutableSelectedDateChanged.value = statsGranularity
        } else if (selectedDate.date == null) {
            mutableDates[statsGranularity] = SelectedDate(error = true)
            mutableSelectedDateChanged.value = statsGranularity
        }
    }

    fun getSelectedDate(statsGranularity: StatsGranularity) = mutableDates[statsGranularity] ?: SelectedDate()

    fun hasSelectedDate(statsGranularity: StatsGranularity): Boolean {
        return mutableDates[statsGranularity]?.date != null
    }

    fun getCurrentDate() = Date()

    data class SelectedDate(val date: Date? = null, val loading: Boolean = false, val error: Boolean = false)
}
