package org.wordpress.android.ui.stats.refresh.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.util.filter
import org.wordpress.android.util.perform
import javax.inject.Inject

class StatsDateSelector
constructor(
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val statsSection: StatsSection
) {
    private val _dateSelectorUiModel = MutableLiveData<DateSelectorUiModel>()
    val dateSelectorData: LiveData<DateSelectorUiModel> = _dateSelectorUiModel

    val selectedDate = selectedDateProvider.selectedDateChanged
            .filter { sectionChange -> sectionChange.selectedSection == this.statsSection }
            .perform {
                if (!it.hasBeenHandled) {
                    updateDateSelector()
                }
            }

    fun updateDateSelector() {
        val shouldShowDateSelection = this.statsSection != INSIGHTS

        val updatedDate = getDateLabelForSection()
        val currentState = dateSelectorData.value
        if (!shouldShowDateSelection && currentState?.isVisible != false) {
            emitValue(currentState, DateSelectorUiModel(false))
        } else {
            val updatedState = DateSelectorUiModel(
                    shouldShowDateSelection,
                    updatedDate,
                    enableSelectPrevious = selectedDateProvider.hasPreviousDate(statsSection),
                    enableSelectNext = selectedDateProvider.hasNextDate(statsSection)
            )
            emitValue(currentState, updatedState)
        }
    }

    private fun emitValue(
        currentState: DateSelectorUiModel?,
        updatedState: DateSelectorUiModel
    ) {
        if (currentState == null ||
                currentState.isVisible != updatedState.isVisible ||
                currentState.date != updatedState.date ||
                currentState.enableSelectNext != updatedState.enableSelectNext ||
                currentState.enableSelectPrevious != updatedState.enableSelectPrevious) {
            _dateSelectorUiModel.value = updatedState
        }
    }

    private fun getDateLabelForSection(): String? {
        return statsDateFormatter.printGranularDate(
                selectedDateProvider.getSelectedDate(statsSection) ?: selectedDateProvider.getCurrentDate(),
                statsSection.toStatsGranularity() ?: DAYS
        )
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

    class Factory
    @Inject constructor(
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter
    ) {
        fun build(statsSection: StatsSection): StatsDateSelector {
            return StatsDateSelector(selectedDateProvider, statsDateFormatter, statsSection)
        }
    }
}
