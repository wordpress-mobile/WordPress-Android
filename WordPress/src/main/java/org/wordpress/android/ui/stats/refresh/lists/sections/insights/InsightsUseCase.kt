package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AllTimeStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.MostPopularInsightsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TodayStatsUseCase
import org.wordpress.android.util.combineMap
import org.wordpress.android.util.merge
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// TODO: This should be a "@SiteScope" of sorts
@Singleton
class InsightsUseCase
@Inject constructor(
    private val statsStore: StatsStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    allTimeStatsUseCase: AllTimeStatsUseCase,
    latestPostSummaryUseCase: LatestPostSummaryUseCase,
    todayStatsUseCase: TodayStatsUseCase,
    followersUseCase: FollowersUseCase,
    commentsUseCase: CommentsUseCase,
    mostPopularInsightsUseCase: MostPopularInsightsUseCase,
    tagsAndCategoriesUseCase: TagsAndCategoriesUseCase
) {
    private val useCases = listOf(
            allTimeStatsUseCase,
            latestPostSummaryUseCase,
            todayStatsUseCase,
            followersUseCase,
            commentsUseCase,
            mostPopularInsightsUseCase,
            tagsAndCategoriesUseCase
    ).associateBy { it.type }

    private val liveData = combineMap(
            useCases.mapValues { entry -> entry.value.liveData }
    )
    private val insights = MutableLiveData<List<InsightsTypes>>()
    val data: LiveData<List<StatsBlock>> = merge(insights, liveData) { insights, map ->
        insights.mapNotNull { map[it] }
    }

    val navigationTarget: LiveData<NavigationTarget> = merge(useCases.map { it.value.navigationTarget })

    suspend fun loadInsightItems(site: SiteModel) {
        loadItems(site, false)
    }

    suspend fun refreshInsightItems(site: SiteModel, forced: Boolean = false) {
        loadItems(site, true, forced)
    }

    private suspend fun loadItems(site: SiteModel, refresh: Boolean, forced: Boolean = false) {
        withContext(bgDispatcher) {
            useCases.values.forEach { block -> launch { block.fetch(site, refresh, forced) } }
            val items = statsStore.getInsights()
            withContext(mainDispatcher) {
                insights.value = items
            }
        }
    }

    fun onCleared() {
        useCases.values.forEach { it.clear() }
    }
}
