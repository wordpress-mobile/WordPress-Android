package org.wordpress.android.ui.stats.refresh.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.util.filter
import org.wordpress.android.util.perform
import javax.inject.Inject

class StatsDateSelector @Inject constructor(
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val statsSectionManager: SelectedSectionManager
) {
    private val _dateSelectorUiModel = MutableLiveData<DateSelectorUiModel>()
    val dateSelectorData: LiveData<DateSelectorUiModel> = _dateSelectorUiModel

    private val granularity
        get() = statsSectionManager.getSelectedStatsGranularity()

    val selectedDate = selectedDateProvider.selectedDateChanged
            .filter { statsGranularity -> statsGranularity == granularity }
            .perform { updateDateSelector() }

    fun updateDateSelector() {
        val statsGranularity = granularity
        val shouldShowDateSelection = statsGranularity != null

        val updatedDate = getDateLabelForSection()
        val currentState = dateSelectorData.value
        if ((!shouldShowDateSelection && currentState?.isVisible != false) || statsGranularity == null) {
            emitValue(currentState, DateSelectorUiModel(false))
        } else {
            val updatedState = DateSelectorUiModel(
                    shouldShowDateSelection,
                    updatedDate,
                    enableSelectPrevious = selectedDateProvider.hasPreviousDate(statsGranularity),
                    enableSelectNext = selectedDateProvider.hasNextDate(statsGranularity)
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
        return granularity?.let {
            statsDateFormatter.printGranularDate(
                    selectedDateProvider.getSelectedDate(it) ?: selectedDateProvider.getCurrentDate(),
                    it
            )
        }
    }

    fun onNextDateSelected() {
        granularity?.let {
            selectedDateProvider.selectNextDate(it)
        }
    }

    fun onPreviousDateSelected() {
        granularity?.let {
            selectedDateProvider.selectPreviousDate(it)
        }
    }
}
