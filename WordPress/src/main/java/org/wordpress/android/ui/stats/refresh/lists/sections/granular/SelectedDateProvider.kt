package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_NEXT_DATE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PREVIOUS_DATE_TAPPED
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toStatsSection
import org.wordpress.android.ui.stats.refresh.utils.trackWithSection
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.filter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val SELECTED_DATE_STATE_KEY = "selected_date_key"

@Singleton
class SelectedDateProvider
@Inject constructor(
    private val statsDateFormatter: StatsDateFormatter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val mutableDates = mutableMapOf(
        DAYS to SelectedDate(loading = true),
        WEEKS to SelectedDate(loading = true),
        MONTHS to SelectedDate(loading = true),
        YEARS to SelectedDate(loading = true)
    )

    private val selectedDateChanged = MutableLiveData<SectionChange?>()

    fun granularSelectedDateChanged(statsSection: StatsSection): LiveData<SectionChange?> {
        return selectedDateChanged.filter { it?.selectedSection == statsSection }
    }

    fun selectDate(date: Date, statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        updateSelectedDate(selectedDate.copy(dateValue = date), statsSection)
    }

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        selectDate(date, statsGranularity.toStatsSection())
    }

    fun selectDate(updatedDate: Date, availableDates: List<Date>, statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        if (selectedDate.dateValue != updatedDate || selectedDate.availableDates != availableDates) {
            updateSelectedDate(
                selectedDate.copy(dateValue = updatedDate, availableDates = availableDates),
                statsSection
            )
        }
    }

    fun selectDate(updatedDate: Date, availableDates: List<Date>, statsGranularity: StatsGranularity) {
        selectDate(updatedDate, availableDates, statsGranularity.toStatsSection())
    }

    fun updateSelectedDate(selectedDate: SelectedDate, statsSection: StatsSection) {
        val currentDate = mutableDates[statsSection]
        mutableDates[statsSection] = selectedDate
        if (selectedDate != currentDate) {
            selectedDateChanged.postValue(SectionChange(statsSection))
        }
    }

    fun setInitialSelectedPeriod(statsGranularity: StatsGranularity, period: String) {
        val updatedDate = statsDateFormatter.parseStatsDate(statsGranularity, period)
        val selectedDate = getSelectedDateState(statsGranularity)
        updateSelectedDate(selectedDate.copy(dateValue = updatedDate), statsGranularity.toStatsSection())
    }

    fun getSelectedDate(statsGranularity: StatsGranularity): Date? {
        return getSelectedDate(statsGranularity.toStatsSection())
    }

    fun getSelectedDate(statsSection: StatsSection): Date? {
        return getSelectedDateState(statsSection).dateValue
    }

    fun getSelectedDateState(statsGranularity: StatsGranularity): SelectedDate {
        return getSelectedDateState(statsGranularity.toStatsSection())
    }

    fun getSelectedDateState(statsSection: StatsSection): SelectedDate {
        return mutableDates[statsSection] ?: SelectedDate(loading = true)
    }

    fun hasPreviousDate(statsSection: StatsSection): Boolean {
        val selectedDate = getSelectedDateState(statsSection)
        return selectedDate.hasData() && selectedDate.getDateIndex() > 0
    }

    fun hasNextDate(statsSection: StatsSection): Boolean {
        val selectedDate = getSelectedDateState(statsSection)
        return selectedDate.hasData() &&
                selectedDate.getDateIndex() < selectedDate.availableDates.size - 1
    }

    fun selectPreviousDate(statsSection: StatsSection) {
        val selectedDateState = getSelectedDateState(statsSection)
        if (selectedDateState.hasData()) {
            analyticsTrackerWrapper.trackWithSection(STATS_PREVIOUS_DATE_TAPPED, statsSection)
            updateSelectedDate(selectedDateState.copy(dateValue = selectedDateState.getPreviousDate()), statsSection)
        }
    }

    fun selectNextDate(statsSection: StatsSection) {
        val selectedDateState = getSelectedDateState(statsSection)
        if (selectedDateState.hasData()) {
            analyticsTrackerWrapper.trackWithSection(STATS_NEXT_DATE_TAPPED, statsSection)
            updateSelectedDate(selectedDateState.copy(dateValue = selectedDateState.getNextDate()), statsSection)
        }
    }

    fun onDateLoadingFailed(statsGranularity: StatsGranularity) {
        onDateLoadingFailed(statsGranularity.toStatsSection())
    }

    fun onDateLoadingFailed(statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        if (selectedDate.dateValue != null && !selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = true, loading = false), statsSection)
        } else if (selectedDate.dateValue == null) {
            updateSelectedDate(SelectedDate(error = true, loading = false), statsSection)
        }
    }

    fun onDateLoadingSucceeded(statsGranularity: StatsGranularity) {
        onDateLoadingSucceeded(statsGranularity.toStatsSection())
    }

    fun onDateLoadingSucceeded(statsSection: StatsSection) {
        val selectedDate = getSelectedDateState(statsSection)
        if (selectedDate.dateValue != null && selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = false, loading = false), statsSection)
        } else if (selectedDate.dateValue == null) {
            updateSelectedDate(SelectedDate(error = false, loading = false), statsSection)
        }
    }

    fun getCurrentDate() = Date()

    fun clear() {
        mutableDates.clear()
        selectedDateChanged.value = null
    }

    fun clear(statsSection: StatsSection) {
        mutableDates[statsSection] = SelectedDate(loading = true)
        selectedDateChanged.value = null
    }

    fun onSaveInstanceState(outState: Bundle) {
        mutableDates.entries.forEach { (key, value) ->
            outState.putParcelable(buildStateKey(key), value)
        }
    }

    fun onRestoreInstanceState(savedState: Bundle) {
        for (period in listOf(DAYS, WEEKS, MONTHS, YEARS)) {
            val selectedDate = savedState.getParcelableCompat<SelectedDate>(buildStateKey(period))
            if (selectedDate != null) {
                mutableDates[period] = selectedDate
            }
        }
    }

    private fun buildStateKey(key: StatsSection) = SELECTED_DATE_STATE_KEY + key

    @Parcelize
    @SuppressLint("ParcelCreator")
    data class SelectedDate(
        val dateValue: Date? = null,
        val availableDates: List<Date> = listOf(),
        val loading: Boolean = false,
        val error: Boolean = false
    ) : Parcelable {
        fun hasData(): Boolean = dateValue != null && availableDates.contains(dateValue)
        fun getDate() = dateValue!!
        fun getDateIndex(): Int {
            return availableDates.indexOf(dateValue)
        }

        fun getPreviousDate(): Date? {
            val dateIndex = getDateIndex()
            return if (dateIndex > 0) {
                availableDates[dateIndex - 1]
            } else {
                null
            }
        }

        fun getNextDate(): Date? {
            val dateIndex = getDateIndex()
            return if (dateIndex > -1 && dateIndex < availableDates.size - 1) {
                availableDates[dateIndex + 1]
            } else {
                null
            }
        }

        companion object : Parceler<SelectedDate> {
            @SuppressLint("ParcelClassLoader")
            override fun create(parcel: Parcel): SelectedDate {
                val dateTimeStamp = parcel.readLong()
                val date = if (dateTimeStamp > -1) {
                    Date(dateTimeStamp)
                } else {
                    null
                }
                val availableTimeStamps = mutableListOf<Any?>()
                ParcelCompat.readList(parcel, availableTimeStamps, null, Any::class.java)
                val availableDates = availableTimeStamps.map { Date(it as Long) }
                val loading = parcel.readValue(null) as Boolean
                val error = parcel.readValue(null) as Boolean
                return SelectedDate(date, availableDates, loading, error)
            }

            override fun SelectedDate.write(parcel: Parcel, flags: Int) {
                parcel.writeLong(dateValue?.time ?: -1)
                parcel.writeList(availableDates.map { it.time })
                parcel.writeValue(loading)
                parcel.writeValue(error)
            }
        }
    }

    data class SectionChange(val selectedSection: StatsSection)
}
