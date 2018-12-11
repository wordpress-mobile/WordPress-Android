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
            DAYS to Date(),
            WEEKS to Date(),
            MONTHS to Date(),
            YEARS to Date()
    )

    private val mutableSelectedDateChanged = MutableLiveData<StatsGranularity>()
    val selectedDateChanged: LiveData<StatsGranularity> = mutableSelectedDateChanged

    fun selectDate(date: Date, statsGranularity: StatsGranularity) {
        mutableDates[statsGranularity] = date
        mutableSelectedDateChanged.value = statsGranularity
    }

    fun getSelectedDate(statsGranularity: StatsGranularity) = mutableDates[statsGranularity] ?: Date()
    fun getCurrentDate() = Date()
}
