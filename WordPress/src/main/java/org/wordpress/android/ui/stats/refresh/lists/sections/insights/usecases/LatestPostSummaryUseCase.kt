package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_ADD_NEW_POST_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_SHARE_POST_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_SINGLE_POST_ACCESSED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class LatestPostSummaryUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val latestPostSummaryMapper: LatestPostSummaryMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatelessUseCase<InsightsLatestPostModel>(LATEST_POST_SUMMARY, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = insightsStore.getLatestPostInsights(site)
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = insightsStore.fetchLatestPostInsights(site, forced)
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(
                    error.message ?: error.type.name
            )
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_latest_post_summary))

    override fun buildUiModel(domainModel: InsightsLatestPostModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_insights_latest_post_summary))
        items.add(latestPostSummaryMapper.buildMessageItem(domainModel, this::onLinkClicked))
        if (domainModel.hasData()) {
            items.add(
                    latestPostSummaryMapper.buildColumnItem(
                            domainModel.postViewsCount,
                            domainModel.postLikeCount,
                            domainModel.postCommentCount
                    )
            )
            if (domainModel.dayViews.isNotEmpty()) {
                items.add(latestPostSummaryMapper.buildBarChartItem(domainModel.dayViews))
            }
        }
        items.add(buildLink(domainModel))
        return items
    }

    private fun InsightsLatestPostModel.hasData() =
            this.postViewsCount > 0 || this.postCommentCount > 0 || this.postLikeCount > 0

    private fun buildLink(model: InsightsLatestPostModel?): Link {
        return when {
            model == null -> Link(
                    R.drawable.ic_create_blue_medium_24dp,
                    R.string.stats_insights_create_post,
                    navigateAction = NavigationAction.create(this::onAddNewPostClick)
            )
            model.hasData() -> Link(
                    text = R.string.stats_insights_view_more,
                    navigateAction = NavigationAction.create(
                            ViewMoreParams(model.postId, model.postTitle, model.postURL),
                            this::onViewMore
                    )
            )
            else -> Link(
                    R.drawable.ic_share_blue_medium_24dp,
                    R.string.stats_insights_share_post,
                    navigateAction = NavigationAction.create(
                            SharePostParams(model.postURL, model.postTitle),
                            this::onSharePost
                    )
            )
        }
    }

    private fun onAddNewPostClick() {
        analyticsTracker.track(STATS_ADD_NEW_POST_TAPPED)
        navigateTo(AddNewPost())
    }

    private fun onViewMore(params: ViewMoreParams) {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_VIEW_POST_DETAILS_TAPPED)
        navigateTo(ViewPostDetailStats(params.postId, params.postTitle, params.postUrl))
    }

    private fun onSharePost(params: SharePostParams) {
        analyticsTracker.track(STATS_SHARE_POST_TAPPED)
        navigateTo(SharePost(params.postUrl, params.postTitle))
    }

    private fun onLinkClicked(params: LinkClickParams) {
        analyticsTracker.track(STATS_SINGLE_POST_ACCESSED)
        navigateTo(ViewPost(params.postId, params.postUrl))
    }

    data class LinkClickParams(val postId: Long, val postUrl: String)
    data class SharePostParams(val postUrl: String, val postTitle: String)
    data class ViewMoreParams(val postId: Long, val postTitle: String, val postUrl: String)
}
