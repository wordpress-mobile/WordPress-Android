package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.mergeNotNull
import javax.inject.Inject
import javax.inject.Named

abstract class StatsViewAllViewModel(
    mainDispatcher: CoroutineDispatcher,
    analyticsTracker: AnalyticsTrackerWrapper,
    protected val useCase: BaseListUseCase
) : StatsListViewModel(mainDispatcher, useCase, analyticsTracker) {
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _showSnackbarMessage = mergeNotNull(
            useCase.snackbarMessage,
            distinct = true,
            singleEvent = true
    )
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    private lateinit var site: SiteModel

    override fun start(site: SiteModel) {
        this.site = site
        launch {
            useCase.loadData(site)
        }
    }

    fun onPullToRefresh() {
        _showSnackbarMessage.value = null
        loadData {
            useCase.refreshData(site, true)
        }
    }

    private fun CoroutineScope.loadData(executeLoading: suspend () -> Unit) = launch {
        _isRefreshing.value = true

        executeLoading()

        _isRefreshing.value = false
    }
}

class StatsViewAllCommentsViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_COMMENTS_USE_CASE) useCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper
) : StatsViewAllViewModel(mainDispatcher, analyticsTracker, useCase)

class StatsViewAllFollowersViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_FOLLOWERS_USE_CASE) useCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper
) : StatsViewAllViewModel(mainDispatcher, analyticsTracker, useCase)

class StatsViewAllTagsAndCategoriesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_TAGS_AND_CATEGORIES_USE_CASE) useCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper
) : StatsViewAllViewModel(mainDispatcher, analyticsTracker, useCase)
