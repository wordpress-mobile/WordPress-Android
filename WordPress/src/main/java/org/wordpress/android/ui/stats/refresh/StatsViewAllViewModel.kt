package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.PostsAndPagesUseCase.PostsAndPagesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase.CommentsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase.FollowersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase.TagsAndCategoriesUseCaseFactory
import org.wordpress.android.util.distinct
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ScopedViewModel
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Named

abstract class StatsViewAllViewModel(
    mainDispatcher: CoroutineDispatcher,
    val bgDispatcher: CoroutineDispatcher,
    val useCase: BaseStatsUseCase<*, *>,
    @StringRes val title: Int
) : ScopedViewModel(mainDispatcher) {
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

class StatsViewAllCommentsViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    useCaseFactory: CommentsUseCaseFactory
) : StatsViewAllViewModel(mainDispatcher, bgDispatcher, useCaseFactory.build(VIEW_ALL), R.string.stats_view_comments)

class StatsViewAllFollowersViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    useCaseFactory: FollowersUseCaseFactory
) : StatsViewAllViewModel(mainDispatcher, bgDispatcher, useCaseFactory.build(VIEW_ALL), R.string.stats_view_followers)

class StatsViewAllTagsAndCategoriesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    useCaseFactory: TagsAndCategoriesUseCaseFactory
) : StatsViewAllViewModel(
        mainDispatcher,
        bgDispatcher,
        useCaseFactory.build(VIEW_ALL),
        R.string.stats_view_tags_and_categories
)

// region ViewAllPostsAndPagesViewModels
class DailyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    useCaseFactory: PostsAndPagesUseCaseFactory
) : StatsViewAllViewModel(
        mainDispatcher,
        bgDispatcher,
        useCaseFactory.build(DAYS, VIEW_ALL),
        R.string.stats_view_top_posts_and_pages
)

class WeeklyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    useCaseFactory: PostsAndPagesUseCaseFactory
) : StatsViewAllViewModel(
        mainDispatcher,
        bgDispatcher,
        useCaseFactory.build(WEEKS, VIEW_ALL),
        R.string.stats_view_top_posts_and_pages
)

class MonthlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    useCaseFactory: PostsAndPagesUseCaseFactory
) : StatsViewAllViewModel(
        mainDispatcher,
        bgDispatcher,
        useCaseFactory.build(MONTHS, VIEW_ALL),
        R.string.stats_view_top_posts_and_pages
)

class YearlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    useCaseFactory: PostsAndPagesUseCaseFactory
) : StatsViewAllViewModel(
        mainDispatcher,
        bgDispatcher,
        useCaseFactory.build(YEARS, VIEW_ALL),
        R.string.stats_view_top_posts_and_pages
)
// endregion
