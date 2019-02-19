package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import javax.inject.Inject
import javax.inject.Named

abstract class StatsViewAllViewModel(
    mainDispatcher: CoroutineDispatcher,
    protected val useCase: BaseListUseCase
) : StatsListViewModel(mainDispatcher, useCase) {
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private lateinit var site: SiteModel
    fun start(site: SiteModel) {
        this.site = site
        loadData {
            useCase.loadData(site)
        }
    }

    fun onPullToRefresh() {
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
    @Named(VIEW_ALL_COMMENTS_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)

class StatsViewAllFollowersViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_FOLLOWERS_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)

class StatsViewAllTagsAndCategoriesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_TAGS_AND_CATEGORIES_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)

// region ViewAllPostsAndPagesViewModels
class DailyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DAILY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)

class WeeklyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(WEEKLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)

class MonthlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(MONTHLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)

class YearlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(YEARLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)
// endregion
