package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.DateSelectorViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.DateSelectorViewModel
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Named

abstract class StatsViewAllViewModel(
    mainDispatcher: CoroutineDispatcher,
    protected val useCase: BaseListUseCase,
    private val dateSelectorViewModel: DateSelectorViewModel,
    @StringRes val title: Int
) : StatsListViewModel(mainDispatcher, useCase) {
    companion object {
        fun get(type: StatsViewType, granularity: StatsGranularity?): Class<out StatsViewAllViewModel> {
            return when (granularity) {
                DAYS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> DailyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                WEEKS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> WeeklyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                MONTHS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> MonthlyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                YEARS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> YearlyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                else -> {
                    when (type) {
                        StatsViewType.FOLLOWERS -> StatsViewAllFollowersViewModel::class.java
                        StatsViewType.COMMENTS -> StatsViewAllCommentsViewModel::class.java
                        StatsViewType.TAGS_AND_CATEGORIES -> StatsViewAllTagsAndCategoriesViewModel::class.java
                        StatsViewType.INSIGHTS_ALL_TIME -> TODO()
                        StatsViewType.INSIGHTS_LATEST_POST_SUMMARY -> TODO()
                        StatsViewType.INSIGHTS_MOST_POPULAR -> TODO()
                        StatsViewType.INSIGHTS_TODAY -> TODO()
                        StatsViewType.PUBLICIZE -> TODO()
                        else -> throw InvalidParameterException("Invalid insights stats type: ${type.name}")
                    }
                }
            }
        }
    }

    val selectedDateChanged = dateSelectorViewModel.selectedDateChanged

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    val dateSelectorUiModel: LiveData<DateSelectorUiModel> = dateSelectorViewModel.uiModel

    private lateinit var site: SiteModel
    private var statsGranularity: StatsGranularity? = null

    fun start(site: SiteModel, granularity: StatsGranularity?) {
        this.site = site
        this.statsGranularity = granularity

        loadData {
            dateSelectorViewModel.updateDateSelector(statsGranularity)
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

    fun onSelectedDateChange() {
        loadData {
            dateSelectorViewModel.updateDateSelector(statsGranularity)
            useCase.refreshData(site)
        }
    }

    fun onNextDateSelected() {
        launch(Dispatchers.Default) {
            statsGranularity?.let { granularity ->
                dateSelectorViewModel.onNextDateSelected(granularity)
            }
        }
    }

    fun onPreviousDateSelected() {
        launch(Dispatchers.Default) {
            statsGranularity?.let { granularity ->
                dateSelectorViewModel.onPreviousDateSelected(granularity)
            }
        }
    }
}

class StatsViewAllCommentsViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_COMMENTS_USE_CASE) useCase: BaseListUseCase,
    dateSelectorViewModel: DateSelectorViewModel
) : StatsViewAllViewModel(mainDispatcher, useCase, dateSelectorViewModel, R.string.stats_view_comments)

class StatsViewAllFollowersViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_FOLLOWERS_USE_CASE) useCase: BaseListUseCase,
    dateSelectorViewModel: DateSelectorViewModel
) : StatsViewAllViewModel(mainDispatcher, useCase, dateSelectorViewModel, R.string.stats_view_followers)

class StatsViewAllTagsAndCategoriesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_TAGS_AND_CATEGORIES_USE_CASE) useCase: BaseListUseCase,
    dateSelectorViewModel: DateSelectorViewModel
) : StatsViewAllViewModel(mainDispatcher, useCase, dateSelectorViewModel, R.string.stats_view_tags_and_categories)

// region ViewAllPostsAndPagesViewModels
class DailyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DAILY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateSelectorViewModel: DateSelectorViewModel
) : StatsViewAllViewModel(mainDispatcher, useCase, dateSelectorViewModel, R.string.stats_view_top_posts_and_pages)

class WeeklyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(WEEKLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateSelectorViewModel: DateSelectorViewModel
) : StatsViewAllViewModel(mainDispatcher, useCase, dateSelectorViewModel, R.string.stats_view_top_posts_and_pages)

class MonthlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(MONTHLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateSelectorViewModel: DateSelectorViewModel
) : StatsViewAllViewModel(mainDispatcher, useCase, dateSelectorViewModel, R.string.stats_view_top_posts_and_pages)

class YearlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(YEARLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateSelectorViewModel: DateSelectorViewModel
) : StatsViewAllViewModel(mainDispatcher, useCase, dateSelectorViewModel, R.string.stats_view_top_posts_and_pages)
// endregion
