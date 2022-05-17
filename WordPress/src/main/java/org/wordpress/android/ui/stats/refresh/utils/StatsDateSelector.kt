package org.wordpress.android.ui.stats.refresh.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.util.perform
import javax.inject.Inject

class StatsDateSelector
constructor(
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val siteProvider: StatsSiteProvider,
    private val statsSection: StatsSection
) {
    private val _dateSelectorUiModel = MutableLiveData<DateSelectorUiModel>()
    val dateSelectorData: LiveData<DateSelectorUiModel> = _dateSelectorUiModel

    val selectedDate = selectedDateProvider.granularSelectedDateChanged(this.statsSection)
            .perform {
                updateDateSelector()
            }

    fun start(startDate: SelectedDate) {
        selectedDateProvider.updateSelectedDate(startDate, statsSection)
    }

    fun updateDateSelector() {
        val shouldShowDateSelection = this.statsSection != INSIGHTS

        val updatedDate = getDateLabelForSection()
        val currentState = dateSelectorData.value
        if (!shouldShowDateSelection && currentState?.isVisible != false) {
            emitValue(currentState, DateSelectorUiModel(false))
        } else {
            val timeZone = statsDateFormatter.printTimeZone(siteProvider.siteModel)
            val updatedState = DateSelectorUiModel(
                    shouldShowDateSelection,
                    updatedDate,
                    enableSelectPrevious = selectedDateProvider.hasPreviousDate(statsSection),
                    enableSelectNext = selectedDateProvider.hasNextDate(statsSection),
                    timeZone = timeZone
            )
            emitValue(currentState, updatedState)
        }
    }

    private fun emitValue(
        currentState: DateSelectorUiModel?,
        updatedState: DateSelectorUiModel
    ) {
        if (currentState != updatedState) {
            _dateSelectorUiModel.value = updatedState
        }
    }

    private fun getDateLabelForSection(): String? {
        return statsDateFormatter.printGranularDate(
                selectedDateProvider.getSelectedDate(statsSection) ?: selectedDateProvider.getCurrentDate(),
                toStatsGranularity()
        )
    }

    private fun toStatsGranularity(): StatsGranularity {
        return when (statsSection) {
            StatsSection.DETAIL,
            StatsSection.TOTAL_LIKES_DETAIL,
            StatsSection.TOTAL_COMMENTS_DETAIL,
            StatsSection.TOTAL_FOLLOWERS_DETAIL,
            StatsSection.INSIGHTS,
            StatsSection.INSIGHT_DETAIL,
            StatsSection.DAYS -> DAYS
            StatsSection.WEEKS -> WEEKS
            StatsSection.MONTHS -> MONTHS
            StatsSection.ANNUAL_STATS,
            StatsSection.YEARS -> YEARS
        }
    }

    fun onNextDateSelected() {
        selectedDateProvider.selectNextDate(statsSection)
    }

    fun onPreviousDateSelected() {
        selectedDateProvider.selectPreviousDate(statsSection)
    }

    fun clear() {
        selectedDateProvider.clear(statsSection)
    }

    fun getSelectedDate(): SelectedDate {
        return selectedDateProvider.getSelectedDateState(statsSection)
    }

    class Factory
    @Inject constructor(
        private val selectedDateProvider: SelectedDateProvider,
        private val siteProvider: StatsSiteProvider,
        private val statsDateFormatter: StatsDateFormatter
    ) {
        fun build(statsSection: StatsSection): StatsDateSelector {
            return StatsDateSelector(
                    selectedDateProvider,
                    statsDateFormatter,
                    siteProvider,
                    statsSection
            )
        }
    }
}
