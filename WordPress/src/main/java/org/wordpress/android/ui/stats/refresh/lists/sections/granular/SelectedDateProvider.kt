package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.util.filter
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

    fun granularSelectedDateChanged(statsGranularity: StatsGranularity): LiveData<StatsGranularity> {
        return selectedDateChanged.filter { it == statsGranularity }
    }

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        val selectedDateIndex = selectedDate.availableDates.indexOf(date)
        if (selectedDate.getDate() != date && selectedDateIndex > -1) {
            updateSelectedDate(selectedDate.copy(index = selectedDateIndex), statsGranularity)
        }
    }

    fun selectDate(updatedIndex: Int, availableDates: List<Date>, statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        if (selectedDate.index != updatedIndex || selectedDate.availableDates != availableDates) {
            updateSelectedDate(
                    selectedDate.copy(index = updatedIndex, availableDates = availableDates),
                    statsGranularity
            )
        }
    }

    private fun updateSelectedDate(selectedDate: SelectedDate, statsGranularity: StatsGranularity) {
        if (mutableDates[statsGranularity] != selectedDate) {
            mutableDates[statsGranularity] = selectedDate
            mutableSelectedDateChanged.postValue(statsGranularity)
        }
    }

    fun getSelectedDate(statsGranularity: StatsGranularity): Date? {
        return getSelectedDateState(statsGranularity).let { selectedDate ->
            selectedDate.index?.let { selectedDate.availableDates.getOrNull(selectedDate.index) }
        }
    }

    fun getSelectedDateState(statsGranularity: StatsGranularity): SelectedDate {
        return mutableDates[statsGranularity] ?: SelectedDate(loading = true)
    }

    fun hasPreviousDate(statsGranularity: StatsGranularity): Boolean {
        val selectedDate = getSelectedDateState(statsGranularity)
        return selectedDate.index != null && selectedDate.hasData() && selectedDate.index > 0
    }

    fun hasNextData(statsGranularity: StatsGranularity): Boolean {
        val selectedDate = getSelectedDateState(statsGranularity)
        return selectedDate.hasData() &&
                selectedDate.index != null &&
                selectedDate.index < selectedDate.availableDates.size - 1
    }

    fun selectPreviousDate(statsGranularity: StatsGranularity) {
        getSelectedDateState(statsGranularity).let { selectedDate ->
            val selectedDateIndex = selectedDate.index
            if (selectedDateIndex != null && selectedDateIndex > 0) {
                updateSelectedDate(selectedDate.copy(index = selectedDate.index - 1), statsGranularity)
            }
        }
    }

    fun selectNextDate(statsGranularity: StatsGranularity) {
        getSelectedDateState(statsGranularity).let { selectedDate ->
            val selectedDateIndex = selectedDate.index
            if (selectedDateIndex != null && selectedDateIndex < selectedDate.availableDates.size - 1) {
                updateSelectedDate(selectedDate.copy(index = selectedDate.index + 1), statsGranularity)
            }
        }
    }

    fun dateLoadingFailed(statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        if (selectedDate.index != null && !selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = true), statsGranularity)
        } else if (selectedDate.index == null) {
            updateSelectedDate(SelectedDate(error = true), statsGranularity)
        }
    }

    fun dateLoadingSucceeded(statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        if (selectedDate.index != null && selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = false), statsGranularity)
        } else if (selectedDate.index == null) {
            updateSelectedDate(SelectedDate(error = false), statsGranularity)
        }
    }

    fun getCurrentDate() = Date()

    fun clear() {
        mutableDates.clear()
    }

    data class SelectedDate(
        val index: Int? = null,
        val availableDates: List<Date> = listOf(),
        val loading: Boolean = false,
        val error: Boolean = false
    ) {
        fun hasData(): Boolean = index != null && availableDates.size > index && index >= 0
        fun getDate(): Date = availableDates[index!!]
    }
}
