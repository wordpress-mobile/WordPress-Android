package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.distinct
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ScopedViewModel

class StatsViewAllViewModel(
    mainDispatcher: CoroutineDispatcher,
    val bgDispatcher: CoroutineDispatcher,
    val useCase: BaseStatsUseCase<*, *>,
    @StringRes val title: Int
) : ScopedViewModel(mainDispatcher) {
    private val mutableSnackbarMessage = MutableLiveData<Int>()

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

    private lateinit var site: SiteModel

    fun start(site: SiteModel) {
        this.site = site
        launch {
            loadData(site, refresh = false, forced = false)
        }
    }

    fun onPullToRefresh() {
        mutableSnackbarMessage.value = null
        loadData {
            loadData(site, refresh = true, forced = true)
        }
    }

    private fun CoroutineScope.loadData(executeLoading: suspend () -> Unit) = launch {
        _isRefreshing.value = true

        executeLoading()

        _isRefreshing.value = false
    }

    private suspend fun loadData(site: SiteModel, refresh: Boolean, forced: Boolean) {
        withContext(bgDispatcher) {
            useCase.fetch(site, refresh, forced)
        }
    }

    override fun onCleared() {
        mutableSnackbarMessage.value = null
        useCase.clear()
    }

    fun onRetryClick(site: SiteModel) {
        loadData {
            loadData(site, refresh = true, forced = true)
        }
    }
}
