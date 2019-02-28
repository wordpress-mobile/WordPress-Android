//package org.wordpress.android.ui.stats.refresh.lists.sections.granular
//
//import android.arch.lifecycle.LiveData
//import android.arch.lifecycle.MutableLiveData
//import org.wordpress.android.fluxc.network.utils.StatsGranularity
//import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
//import javax.inject.Inject
//
//class DateSelectorViewModel
//@Inject constructor(
//    private val selectedDateProvider: SelectedDateProvider,
//    private val dateFormatter: StatsDateFormatter
//) {
//    val selectedDateChanged = selectedDateProvider.selectedDateChanged
//
//    private val _uiModel = MutableLiveData<DateSelectorUiModel>()
//    val uiModel: LiveData<DateSelectorUiModel> = _uiModel
//
//    fun updateDateSelector(statsGranularity: StatsGranularity?) {
//        val shouldShowDateSelection = statsGranularity != null
//        if (shouldShowDateSelection) {
//            statsGranularity?.let { granularity ->
//                val updatedDate = getDateLabelForSection(granularity)
//                val currentState = _uiModel.value
//                if (!shouldShowDateSelection && currentState?.isVisible != false) {
//                    emitValue(currentState,
//                            DateSelectorUiModel(
//                                    false
//                            )
//                    )
//                } else {
//                    val updatedState = DateSelectorUiModel(
//                            shouldShowDateSelection,
//                            updatedDate,
//                            enableSelectPrevious = selectedDateProvider.hasPreviousDate(granularity),
//                            enableSelectNext = selectedDateProvider.hasNextData(granularity)
//                    )
//                    emitValue(currentState, updatedState)
//                }
//            }
//        } else {
//            emitValue(_uiModel.value, DateSelectorUiModel(false))
//        }
//    }
//
//    fun onNextDateSelected(granularity: StatsGranularity) {
//        selectedDateProvider.selectNextDate(granularity)
//    }
//
//    fun onPreviousDateSelected(granularity: StatsGranularity) {
//        selectedDateProvider.selectPreviousDate(granularity)
//    }
//
//    private fun emitValue(
//        currentState: DateSelectorUiModel?,
//        updatedState: DateSelectorUiModel
//    ) {
//        if (currentState == null ||
//            currentState.isVisible != updatedState.isVisible ||
//            currentState.date != updatedState.date ||
//            currentState.enableSelectNext != updatedState.enableSelectNext ||
//            currentState.enableSelectPrevious != updatedState.enableSelectPrevious) {
//            _uiModel.value = updatedState
//        }
//    }
//
//    private fun getDateLabelForSection(granularity: StatsGranularity): String? {
//        return dateFormatter.printGranularDate(
//                selectedDateProvider.getSelectedDate(granularity) ?: selectedDateProvider.getCurrentDate(),
//                granularity
//        )
//    }
//}
