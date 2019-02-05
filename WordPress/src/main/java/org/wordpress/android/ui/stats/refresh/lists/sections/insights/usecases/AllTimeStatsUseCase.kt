package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class AllTimeStatsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val statsDateFormatter: StatsDateFormatter
) : StatelessUseCase<InsightsAllTimeModel>(ALL_TIME_STATS, mainDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_all_time_stats))

    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = insightsStore.getAllTimeInsights(site)
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = insightsStore.fetchAllTimeInsights(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildUiModel(domainModel: InsightsAllTimeModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_all_time_stats))

        val hasPosts = domainModel.posts > 0
        val hasViews = domainModel.views > 0
        val hasVisitors = domainModel.visitors > 0
        val hasViewsBestDayTotal = domainModel.viewsBestDayTotal > 0
        if (!hasPosts && !hasViews && !hasVisitors && !hasViewsBestDayTotal) {
            items.add(Empty())
        } else {
            if (hasPosts) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_posts_grey_dark_24dp,
                                textResource = R.string.posts,
                                value = domainModel.posts.toFormattedString(),
                                showDivider = hasViews || hasVisitors || hasViewsBestDayTotal
                        )
                )
            }
            if (hasViews) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_visible_on_grey_dark_24dp,
                                textResource = R.string.stats_views,
                                value = domainModel.views.toFormattedString(),
                                showDivider = hasVisitors || hasViewsBestDayTotal
                        )
                )
            }
            if (hasVisitors) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_user_grey_dark_24dp,
                                textResource = R.string.stats_visitors,
                                value = domainModel.visitors.toFormattedString(),
                                showDivider = hasViewsBestDayTotal
                        )
                )
            }
            if (hasViewsBestDayTotal) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_trophy_grey_dark_24dp,
                                textResource = R.string.stats_insights_best_ever,
                                subText = statsDateFormatter.printDate(domainModel.viewsBestDay),
                                value = domainModel.viewsBestDayTotal.toFormattedString(),
                                showDivider = false
                        )
                )
            }
        }
        return items
    }
}
