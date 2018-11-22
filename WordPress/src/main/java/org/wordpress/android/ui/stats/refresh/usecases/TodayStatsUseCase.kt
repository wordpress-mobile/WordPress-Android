package org.wordpress.android.ui.stats.refresh.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.BlockListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class TodayStatsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore
) : BaseInsightsUseCase(TODAY_STATS, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
        val dbModel = insightsStore.getTodayInsights(site)
        return dbModel?.let { loadTodayStatsItem(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): InsightsItem? {
        val response = insightsStore.fetchTodayInsights(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> createFailedItem(R.string.stats_insights_today_stats, error.message ?: error.type.name)
            else -> model?.let { loadTodayStatsItem(model) }
        }
    }

    private fun loadTodayStatsItem(model: VisitsModel): InsightsItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_today_stats))

        val hasViews = model.views > 0
        val hasVisitors = model.visitors > 0
        val hasLikes = model.likes > 0
        val hasComments = model.comments > 0
        if (!hasViews && !hasVisitors && !hasLikes && !hasComments) {
            items.add(Empty)
        } else {
            if (hasViews) {
                items.add(
                        Item(
                                R.drawable.ic_visible_on_grey_dark_24dp,
                                textResource = R.string.stats_views,
                                value = model.views.toFormattedString(),
                                showDivider = hasVisitors || hasLikes || hasComments
                        )
                )
            }
            if (hasVisitors) {
                items.add(
                        Item(
                                R.drawable.ic_user_grey_dark_24dp,
                                textResource = R.string.stats_visitors,
                                value = model.visitors.toFormattedString(),
                                showDivider = hasLikes || hasComments
                        )
                )
            }
            if (hasLikes) {
                items.add(
                        Item(
                                R.drawable.ic_star_grey_dark_24dp,
                                textResource = R.string.stats_likes,
                                value = model.likes.toFormattedString(),
                                showDivider = hasComments
                        )
                )
            }
            if (hasComments) {
                items.add(
                        Item(
                                R.drawable.ic_comment_grey_dark_24dp,
                                textResource = R.string.stats_comments,
                                value = model.comments.toFormattedString(),
                                showDivider = false
                        )
                )
            }
        }
        return createDataItem(items)
    }
}
