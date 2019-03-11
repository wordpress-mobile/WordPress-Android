package org.wordpress.android.ui.stats.refresh.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.util.map
import javax.inject.Inject

class StatsDateSelector @Inject constructor(
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val statsSectionManager: SelectedSectionManager
) {
    private val _dateSelectorUiModel = MutableLiveData<DateSelectorUiModel>()
    val dateSelectorData: LiveData<DateSelectorUiModel> = _dateSelectorUiModel

    val selectedDateChanged: LiveData<StatsGranularity> = selectedDateProvider.selectedDateChanged.map { granularity ->
        if (granularity == statsSectionManager.getSelectedStatsGranularity()) {
            updateDateSelector(granularity)
        }
        return@map granularity
    }

    fun updateDateSelector(statsGranularity: StatsGranularity?) {
        val shouldShowDateSelection = statsGranularity != null

        val updatedDate = getDateLabelForSection(statsGranularity)
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

    private fun getDateLabelForSection(statsGranularity: StatsGranularity?): String? {
        return statsGranularity?.let {
            statsDateFormatter.printGranularDate(
                    selectedDateProvider.getSelectedDate(statsGranularity) ?: selectedDateProvider.getCurrentDate(),
                    statsGranularity
            )
        }
    }

    fun onNextDateSelected(granularity: StatsGranularity) {
        selectedDateProvider.selectNextDate(granularity)
    }

    fun onPreviousDateSelected(granularity: StatsGranularity) {
        selectedDateProvider.selectPreviousDate(granularity)
    }
}
