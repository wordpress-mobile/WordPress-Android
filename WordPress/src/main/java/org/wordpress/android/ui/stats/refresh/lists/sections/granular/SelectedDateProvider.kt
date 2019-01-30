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
        updateSelectedDate(SelectedDate(date), statsGranularity)
    }

    fun dateLoadingFailed(statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDate(statsGranularity)
        if (selectedDate.date != null && !selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = true), statsGranularity)
        } else if (selectedDate.date == null) {
            updateSelectedDate(SelectedDate(error = true), statsGranularity)
        }
    }

    fun getSelectedDate(statsGranularity: StatsGranularity) = mutableDates[statsGranularity] ?: SelectedDate()

    fun getCurrentDate() = Date()

    fun clear() {
        mutableDates.clear()
    }

    private fun updateSelectedDate(selectedDate: SelectedDate, statsGranularity: StatsGranularity) {
        val previousDate = getSelectedDate(statsGranularity)
        if (selectedDate != previousDate) {
            mutableDates[statsGranularity] = selectedDate
            mutableSelectedDateChanged.value = statsGranularity
        }
    }

    data class SelectedDate(val date: Date? = null, val loading: Boolean = false, val error: Boolean = false)
}
