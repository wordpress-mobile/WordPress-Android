package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_DATE_TAPPED_FORWARD
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_DATE_TAPPED_BACKWARD
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.trackWithGranularity
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.readListCompat
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
        StatsGranularity.DAYS to SelectedDate(loading = true),
        StatsGranularity.WEEKS to SelectedDate(loading = true),
        StatsGranularity.MONTHS to SelectedDate(loading = true),
        StatsGranularity.YEARS to SelectedDate(loading = true)
    )

    private val selectedDateChanged = MutableLiveData<GranularityChange?>()

    fun granularSelectedDateChanged(): LiveData<GranularityChange?> = selectedDateChanged

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        updateSelectedDate(selectedDate.copy(dateValue = date), statsGranularity)
    }

    fun selectDate(updatedDate: Date, availableDates: List<Date>, statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        if (selectedDate.dateValue != updatedDate || selectedDate.availableDates != availableDates) {
            updateSelectedDate(
                selectedDate.copy(dateValue = updatedDate, availableDates = availableDates),
                statsGranularity
            )
        }
    }

    fun updateSelectedDate(selectedDate: SelectedDate, statsGranularity: StatsGranularity) {
        val currentDate = mutableDates[statsGranularity]
        mutableDates[statsGranularity] = selectedDate
        if (selectedDate != currentDate) {
            selectedDateChanged.postValue(GranularityChange(statsGranularity))
        }
    }

    fun setInitialSelectedPeriod(statsGranularity: StatsGranularity, period: String) {
        val updatedDate = statsDateFormatter.parseStatsDate(statsGranularity, period)
        val selectedDate = getSelectedDateState(statsGranularity)
        updateSelectedDate(selectedDate.copy(dateValue = updatedDate), statsGranularity)
    }

    fun getSelectedDate(statsGranularity: StatsGranularity): Date? {
        return getSelectedDateState(statsGranularity).dateValue
    }

    fun getSelectedDateState(statsGranularity: StatsGranularity): SelectedDate {
        return mutableDates[statsGranularity] ?: SelectedDate(loading = true)
    }

    fun hasPreviousDate(statsGranularity: StatsGranularity): Boolean {
        val selectedDate = getSelectedDateState(statsGranularity)
        return selectedDate.hasData() && selectedDate.getDateIndex() > 0
    }

    fun hasNextDate(statsGranularity: StatsGranularity): Boolean {
        val selectedDate = getSelectedDateState(statsGranularity)
        return selectedDate.hasData() &&
                selectedDate.getDateIndex() < selectedDate.availableDates.size - 1
    }

    fun selectPreviousDate(statsGranularity: StatsGranularity) {
        val selectedDateState = getSelectedDateState(statsGranularity)
        if (selectedDateState.hasData()) {
            analyticsTrackerWrapper.trackWithGranularity(STATS_DATE_TAPPED_BACKWARD, statsGranularity)
            updateSelectedDate(
                selectedDateState.copy(dateValue = selectedDateState.getPreviousDate()),
                statsGranularity
            )
        }
    }

    fun selectNextDate(statsGranularity: StatsGranularity) {
        val selectedDateState = getSelectedDateState(statsGranularity)
        if (selectedDateState.hasData()) {
            analyticsTrackerWrapper.trackWithGranularity(STATS_DATE_TAPPED_FORWARD, statsGranularity)
            updateSelectedDate(selectedDateState.copy(dateValue = selectedDateState.getNextDate()), statsGranularity)
        }
    }

    fun onDateLoadingFailed(statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        if (selectedDate.dateValue != null && !selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = true, loading = false), statsGranularity)
        } else if (selectedDate.dateValue == null) {
            updateSelectedDate(SelectedDate(error = true, loading = false), statsGranularity)
        }
    }

    fun onDateLoadingSucceeded(statsGranularity: StatsGranularity) {
        val selectedDate = getSelectedDateState(statsGranularity)
        if (selectedDate.dateValue != null && selectedDate.error) {
            updateSelectedDate(selectedDate.copy(error = false, loading = false), statsGranularity)
        } else if (selectedDate.dateValue == null) {
            updateSelectedDate(SelectedDate(error = false, loading = false), statsGranularity)
        }
    }

    fun getCurrentDate() = Date()

    fun clear() {
        mutableDates.clear()
        selectedDateChanged.value = null
    }

    fun clear(statsGranularity: StatsGranularity) {
        mutableDates[statsGranularity] = SelectedDate(loading = true)
        selectedDateChanged.value = null
    }

    fun onSaveInstanceState(outState: Bundle) {
        mutableDates.entries.forEach { (key, value) ->
            outState.putParcelable(buildStateKey(key), value)
        }
    }

    fun onRestoreInstanceState(savedState: Bundle) {
        for (period in listOf(
            StatsGranularity.DAYS,
            StatsGranularity.WEEKS,
            StatsGranularity.MONTHS,
            StatsGranularity.YEARS
        )) {
            val selectedDate = savedState.getParcelableCompat<SelectedDate>(buildStateKey(period))
            if (selectedDate != null) {
                mutableDates[period] = selectedDate
            }
        }
    }

    private fun buildStateKey(key: StatsGranularity) = SELECTED_DATE_STATE_KEY + key

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
                parcel.readListCompat(availableTimeStamps, null)
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

    data class GranularityChange(val selectedGranularity: StatsGranularity)
}
