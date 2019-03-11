package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.distinct
import org.wordpress.android.util.map
import org.wordpress.android.util.mapNullable
import org.wordpress.android.viewmodel.ScopedViewModel

class StatsViewAllViewModel(
    mainDispatcher: CoroutineDispatcher,
    val bgDispatcher: CoroutineDispatcher,
    val useCase: BaseStatsUseCase<*, *>,
    private val statsSiteProvider: StatsSiteProvider,
    private val dateSelector: StatsDateSelector,
    @StringRes val title: Int
) : ScopedViewModel(mainDispatcher) {
    private val mutableSnackbarMessage = MutableLiveData<Int>()

    val selectedDate = dateSelector.selectedDate

    val dateSelectorData: LiveData<DateSelectorUiModel> = dateSelector.dateSelectorData.mapNullable {
        it ?: DateSelectorUiModel(false)
    }

    val navigationTarget: LiveData<NavigationTarget> = useCase.navigationTarget

    val data: LiveData<StatsBlock> = useCase.liveData.map { useCaseModel ->
        when (useCaseModel.state) {
            SUCCESS -> StatsBlock.Success(useCaseModel.type, useCaseModel.data ?: listOf())
            ERROR -> StatsBlock.Error(useCaseModel.type, useCaseModel.stateData ?: useCaseModel.data ?: listOf())
            LOADING -> StatsBlock.Loading(useCaseModel.type, useCaseModel.data ?: useCaseModel.stateData ?: listOf())
            EMPTY -> StatsBlock.EmptyBlock(useCaseModel.type, useCaseModel.stateData ?: useCaseModel.data ?: listOf())
        }
    }.distinct()

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = mutableSnackbarMessage.map {
        SnackbarMessageHolder(it)
    }

    val toolbarHasShadow = dateSelectorData.map { !it.isVisible }

    fun start() {
        launch {
            loadData(refresh = false, forced = false)
            dateSelector.updateDateSelector()
        }
        dateSelector.updateDateSelector()
    }

    fun onPullToRefresh() {
        mutableSnackbarMessage.value = null
        refreshData()
    }

    private fun CoroutineScope.loadData(executeLoading: suspend () -> Unit) = launch {
        _isRefreshing.value = true

        executeLoading()

        _isRefreshing.value = false
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean) {
        withContext(bgDispatcher) {
            useCase.fetch(refresh, forced)
        }
    }

    override fun onCleared() {
        mutableSnackbarMessage.value = null
        useCase.clear()
    }

    fun onRetryClick() {
        loadData {
            loadData(refresh = true, forced = true)
        }
    }

    fun onNextDateSelected() {
        launch(Dispatchers.Default) {
            dateSelector.onNextDateSelected()
        }
    }

    fun onPreviousDateSelected() {
        launch(Dispatchers.Default) {
            dateSelector.onPreviousDateSelected()
        }
    }

    fun onDateChanged() {
        refreshData()
    }

    private fun refreshData() {
        if (statsSiteProvider.hasLoadedSite()) {
            loadData {
                loadData(refresh = true, forced = true)
            }
        } else {
            mutableSnackbarMessage.value = R.string.stats_site_not_loaded_yet
        }
    }
}
