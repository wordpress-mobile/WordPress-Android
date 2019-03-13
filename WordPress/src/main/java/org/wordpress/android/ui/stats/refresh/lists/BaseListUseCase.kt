package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.DistinctMutableLiveData
import org.wordpress.android.util.PackageUtils
import org.wordpress.android.util.combineMap
import org.wordpress.android.util.distinct
import org.wordpress.android.util.map
import org.wordpress.android.util.mergeNotNull

class BaseListUseCase(
    private val bgDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val statsSectionManager: SelectedSectionManager,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val statsSiteProvider: StatsSiteProvider,
    private val useCases: List<BaseStatsUseCase<*, *>>,
    private val getStatsTypes: suspend () -> List<StatsTypes>,
    private val mapUiModel: (useCaseModels: List<UseCaseModel>, showError: (Int) -> Unit) -> UiModel
) {
    private val blockListData = combineMap(
            useCases.associateBy { it.type }.mapValues { entry -> entry.value.liveData }
    )
    private val statsTypes = DistinctMutableLiveData<List<StatsTypes>>(listOf())
    val data: MediatorLiveData<UiModel> = mergeNotNull(statsTypes, blockListData) { insights, map ->
        insights.mapNotNull {
            if (map.containsKey(it)) {
                map[it]
            } else {
                null
            }
        }
    }.map { useCaseModels ->
        mapUiModel(useCaseModels) { message ->
            mutableSnackbarMessage.postValue(message)
        }
    }.distinct()

    val navigationTarget: LiveData<NavigationTarget> = mergeNotNull(
            useCases.map { it.navigationTarget },
            distinct = false
    )

    private val mutableShowDateSelector = MutableLiveData<DateSelectorUiModel>()
    val showDateSelector: LiveData<DateSelectorUiModel> = mutableShowDateSelector

    private val mutableSnackbarMessage = MutableLiveData<Int>()
    val snackbarMessage: LiveData<SnackbarMessageHolder> = mutableSnackbarMessage.map {
        SnackbarMessageHolder(it)
    }

    suspend fun loadData() {
        loadData(refresh = false, forced = false)
    }

    suspend fun refreshData(forced: Boolean = false) {
        loadData(true, forced)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean) {
        if (statsSiteProvider.hasLoadedSite()) {
            withContext(bgDispatcher) {
                if (PackageUtils.isDebugBuild() && useCases.distinctBy { it.type }.size < useCases.size) {
                    throw RuntimeException("Duplicate stats type in a use case")
                }
                useCases.forEach { block -> launch { block.fetch(refresh, forced) } }
                val items = getStatsTypes()
                withContext(mainDispatcher) {
                    statsTypes.value = items
                }
            }
        } else {
            mutableSnackbarMessage.value = R.string.stats_site_not_loaded_yet
        }
    }

    fun onCleared() {
        mutableSnackbarMessage.value = null
        statsTypes.clear()
        blockListData.value = null
        useCases.forEach { it.clear() }
        data.value = null
    }

    suspend fun onDateChanged(statsGranularity: StatsGranularity?) {
        updateDateSelector(statsGranularity)
        refreshData()
    }

    fun updateDateSelector(statsGranularity: StatsGranularity?) {
        val shouldShowDateSelection = statsGranularity != null

        val updatedDate = getDateLabelForSection(statsGranularity)
        val currentState = showDateSelector.value
        if ((!shouldShowDateSelection && currentState?.isVisible != false) || statsGranularity == null) {
            emitValue(currentState, DateSelectorUiModel(false))
        } else {
            val updatedState = DateSelectorUiModel(
                    shouldShowDateSelection,
                    updatedDate,
                    enableSelectPrevious = selectedDateProvider.hasPreviousDate(statsGranularity),
                    enableSelectNext = selectedDateProvider.hasNextData(statsGranularity)
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
            mutableShowDateSelector.value = updatedState
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

    fun onNextDateSelected() {
        statsSectionManager.getSelectedStatsGranularity()?.let { statsGranularity ->
            selectedDateProvider.selectNextDate(statsGranularity)
        }
    }

    fun onPreviousDateSelected() {
        statsSectionManager.getSelectedStatsGranularity()?.let { statsGranularity ->
            selectedDateProvider.selectPreviousDate(statsGranularity)
        }
    }
}
