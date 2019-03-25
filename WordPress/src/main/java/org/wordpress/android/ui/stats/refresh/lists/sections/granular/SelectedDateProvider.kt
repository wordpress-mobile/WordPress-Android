package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.utils.toStatsSection
import org.wordpress.android.util.Event
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

    private val mutableSelectedDateChanged = MutableLiveData<SectionChange>()
    val selectedDateChanged: LiveData<SectionChange> = mutableSelectedDateChanged

    fun granularSelectedDateChanged(statsGranularity: StatsGranularity): LiveData<SectionChange> {
        return selectedDateChanged.filter { it.selectedSection == statsGranularity.toStatsSection() }
    }

    fun granularSelectedDateChanged(statsSection: StatsSection): LiveData<SectionChange> {
        return selectedDateChanged.filter { it.selectedSection == statsSection }
    }

    fun selectDate(date: Date, statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        val selectedDateIndex = selectedDate.availableDates.indexOf(date)
        if (selectedDate.getDate() != date && selectedDateIndex > -1) {
            updateSelectedDate(selectedDate.copy(index = selectedDateIndex), statsSection)
        }
    }

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        selectDate(date, statsGranularity.toStatsSection())
    }

    fun selectDate(updatedIndex: Int, availableDates: List<Date>, statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        if (selectedDate.index != updatedIndex || selectedDate.availableDates != availableDates) {
            updateSelectedDate(
                    selectedDate.copy(index = updatedIndex, availableDates = availableDates),
                    statsSection
            )
        }
    }

    fun selectDate(updatedIndex: Int, availableDates: List<Date>, statsGranularity: StatsGranularity) {
        selectDate(updatedIndex, availableDates, statsGranularity.toStatsSection())
    }

    private fun updateSelectedDate(selectedDate: SelectedDate, statsSection: StatsSection) {
        if (mutableDates[statsSection] != selectedDate) {
            mutableDates[statsSection] = selectedDate
            mutableSelectedDateChanged.postValue(SectionChange(statsSection))
        }
    }

    fun getSelectedDate(statsGranularity: StatsGranularity): Date? {
        return getSelectedDate(statsGranularity.toStatsSection())
    }

    fun getSelectedDate(statsSection: StatsSection): Date? {
        return getSelectedDateState(statsSection).let { selectedDate ->
            selectedDate.index?.let { selectedDate.availableDates.getOrNull(selectedDate.index) }
        }
    }

    fun getSelectedDateState(statsGranularity: StatsGranularity): SelectedDate {
        return getSelectedDateState(statsGranularity.toStatsSection())
    }

    fun getSelectedDateState(statsSection: StatsSection): SelectedDate {
        return mutableDates[statsSection] ?: SelectedDate(loading = true)
    }

    fun hasPreviousDate(statsSection: StatsSection): Boolean {
        val selectedDate = getSelectedDateState(statsSection)
        return selectedDate.index != null && selectedDate.hasData() && selectedDate.index > 0
    }

    fun hasNextDate(statsSection: StatsSection): Boolean {
        val selectedDate = getSelectedDateState(statsSection)
        return selectedDate.hasData() &&
                selectedDate.index != null &&
                selectedDate.index < selectedDate.availableDates.size - 1
    }

    fun selectPreviousDate(statsSection: StatsSection) {
        getSelectedDateState(statsSection).let { selectedDate ->
            val selectedDateIndex = selectedDate.index
            if (selectedDateIndex != null && selectedDateIndex > 0) {
                updateSelectedDate(selectedDate.copy(index = selectedDate.index - 1), statsSection)
            }
        }
    }

    fun selectNextDate(statsSection: StatsSection) {
        getSelectedDateState(statsSection).let { selectedDate ->
            val selectedDateIndex = selectedDate.index
            if (selectedDateIndex != null && selectedDateIndex < selectedDate.availableDates.size - 1) {
                updateSelectedDate(selectedDate.copy(index = selectedDate.index + 1), statsSection)
            }
        }
    }

    fun dateLoadingFailed(statsGranularity: StatsGranularity) {
        dateLoadingFailed(statsGranularity.toStatsSection())
    }

    fun dateLoadingFailed(statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        if (selectedDate.index != null && !selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = true), statsSection)
        } else if (selectedDate.index == null) {
            updateSelectedDate(SelectedDate(error = true), statsSection)
        }
    }

    fun dateLoadingSucceeded(statsGranularity: StatsGranularity) {
        dateLoadingSucceeded(statsGranularity.toStatsSection())
    }

    fun dateLoadingSucceeded(statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        if (selectedDate.index != null && selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = false), statsSection)
        } else if (selectedDate.index == null) {
            updateSelectedDate(SelectedDate(error = false), statsSection)
        }
    }

    fun getCurrentDate() = Date()

    fun clear() {
        mutableDates.clear()
        mutableSelectedDateChanged.value = null
    }

    fun clear(statsSection: StatsSection) {
        mutableDates[statsSection] = SelectedDate(loading = true)
        mutableSelectedDateChanged.value = null
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

    data class SectionChange(val selectedSection: StatsSection) : Event()
}
