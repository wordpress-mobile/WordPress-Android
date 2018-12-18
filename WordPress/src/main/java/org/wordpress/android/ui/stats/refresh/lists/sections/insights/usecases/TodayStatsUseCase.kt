package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class TodayStatsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore
) : StatelessUseCase<VisitsModel>(TODAY_STATS, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = insightsStore.getTodayInsights(site)
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = insightsStore.fetchTodayInsights(site, forced)
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_today_stats))

    override fun buildUiModel(domainModel: VisitsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_today_stats))

        val hasViews = domainModel.views > 0
        val hasVisitors = domainModel.visitors > 0
        val hasLikes = domainModel.likes > 0
        val hasComments = domainModel.comments > 0
        if (!hasViews && !hasVisitors && !hasLikes && !hasComments) {
            items.add(Empty)
        } else {
            if (hasViews) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_visible_on_grey_dark_24dp,
                                textResource = R.string.stats_views,
                                value = domainModel.views.toFormattedString(),
                                showDivider = hasVisitors || hasLikes || hasComments
                        )
                )
            }
            if (hasVisitors) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_user_grey_dark_24dp,
                                textResource = R.string.stats_visitors,
                                value = domainModel.visitors.toFormattedString(),
                                showDivider = hasLikes || hasComments
                        )
                )
            }
            if (hasLikes) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_star_grey_dark_24dp,
                                textResource = R.string.stats_likes,
                                value = domainModel.likes.toFormattedString(),
                                showDivider = hasComments
                        )
                )
            }
            if (hasComments) {
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_comment_grey_dark_24dp,
                                textResource = R.string.stats_comments,
                                value = domainModel.comments.toFormattedString(),
                                showDivider = false
                        )
                )
            }
        }
        return items
    }
}
