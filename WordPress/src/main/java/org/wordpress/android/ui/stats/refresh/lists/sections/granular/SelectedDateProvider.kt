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
    private val mutableDates = mutableMapOf<StatsGranularity, SelectedDate>()

    private val mutableSelectedDateChanged = MutableLiveData<StatsGranularity>()
    val selectedDateChanged: LiveData<StatsGranularity> = mutableSelectedDateChanged

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        val selectedDate = mutableDates[statsGranularity]
        val selectedDateIndex = selectedDate?.availableDates?.indexOf(date) ?: -1
        if (selectedDate != null && selectedDate.getDate() != date && selectedDateIndex > -1) {
            selectDate(selectedDate.copy(index = selectedDateIndex), statsGranularity)
        }
    }

    fun selectDate(selectedDate: SelectedDate, statsGranularity: StatsGranularity) {
        if (selectedDate != mutableDates[statsGranularity]) {
            mutableDates[statsGranularity] = selectedDate
            mutableSelectedDateChanged.postValue(statsGranularity)
        }
    }

    fun getSelectedDate(statsGranularity: StatsGranularity): Date? {
        return mutableDates[statsGranularity]?.let { selectedDate ->
            selectedDate.availableDates.getOrNull(selectedDate.index)
        }
    }

    fun hasPreviousDate(statsGranularity: StatsGranularity): Boolean {
        val selectedDate = mutableDates[statsGranularity]
        return selectedDate != null && selectedDate.hasData() && selectedDate.index > 0
    }

    fun hasNextData(statsGranularity: StatsGranularity): Boolean {
        val selectedDate = mutableDates[statsGranularity]
        return selectedDate != null &&
                selectedDate.hasData() &&
                selectedDate.index < selectedDate.availableDates.size - 1
    }

    fun selectPreviousDate(statsGranularity: StatsGranularity) {
        mutableDates[statsGranularity]?.let { selectedDate ->
            if (selectedDate.index > 0) {
                selectDate(selectedDate.copy(index = selectedDate.index - 1), statsGranularity)
            }
        }
    }

    fun selectNextDate(statsGranularity: StatsGranularity) {
        mutableDates[statsGranularity]?.let { selectedDate ->
            if (selectedDate.index < selectedDate.availableDates.size - 1) {
                selectDate(selectedDate.copy(index = selectedDate.index + 1), statsGranularity)
            }
        }
    }

    fun getCurrentDate() = Date()
    fun clear() {
        mutableDates.clear()
    }

    data class SelectedDate(
        val index: Int,
        val availableDates: List<Date> = listOf()
    ) {
        fun hasData(): Boolean = availableDates.size > index && index >= 0
        fun getDate(): Date = availableDates[index]
    }
}
