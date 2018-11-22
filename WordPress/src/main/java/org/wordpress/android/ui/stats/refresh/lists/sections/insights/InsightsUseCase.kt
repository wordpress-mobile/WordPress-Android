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
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks.AllTimeStatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks.CommentsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks.FollowersBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks.LatestPostSummaryBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks.MostPopularInsightsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks.TagsAndCategoriesBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks.TodayStatsBlock
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
    allTimeStatsBlock: AllTimeStatsBlock,
    latestPostSummaryBlock: LatestPostSummaryBlock,
    todayStatsBlock: TodayStatsBlock,
    followersBlock: FollowersBlock,
    commentsBlock: CommentsBlock,
    mostPopularInsightsBlock: MostPopularInsightsBlock,
    tagsAndCategoriesBlock: TagsAndCategoriesBlock
) {
    private val blocks = listOf(
            allTimeStatsBlock,
            latestPostSummaryBlock,
            todayStatsBlock,
            followersBlock,
            commentsBlock,
            mostPopularInsightsBlock,
            tagsAndCategoriesBlock
    ).associateBy { it.type }

    private val liveData = combineMap(
            blocks.mapValues { entry -> entry.value.liveData }
    )
    private val insights = MutableLiveData<List<InsightsTypes>>()
    val data: LiveData<List<StatsListItem>> = merge(insights, liveData) { insights, map ->
        insights.mapNotNull { map[it] }
    }

    val navigationTarget: LiveData<NavigationTarget> = merge(blocks.map { it.value.navigationTarget })

    suspend fun loadInsightItems(site: SiteModel) {
        loadItems(site, false)
    }

    suspend fun refreshInsightItems(site: SiteModel, forced: Boolean = false) {
        loadItems(site, true, forced)
    }

    private suspend fun loadItems(site: SiteModel, refresh: Boolean, forced: Boolean = false) {
        withContext(bgDispatcher) {
            blocks.values.forEach { block -> launch { block.fetch(site, refresh, forced) } }
            val items = statsStore.getInsights()
            withContext(mainDispatcher) {
                insights.value = items
            }
        }
    }

    fun onCleared() {
        blocks.values.forEach { it.clear() }
    }
}
