package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_ADD_NEW_POST_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_POST_ITEM_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_SHARE_POST_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_VIEW_POST_DETAILS_TAPPED
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.stats.insights.LatestPostInsightsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.utils.HUNDRED_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class LatestPostSummaryUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val latestPostStore: LatestPostInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val latestPostSummaryMapper: LatestPostSummaryMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatelessUseCase<InsightsLatestPostModel>(LATEST_POST_SUMMARY, mainDispatcher) {
    override suspend fun loadCachedData(): InsightsLatestPostModel? {
        return latestPostStore.getLatestPostInsights(statsSiteProvider.siteModel)
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<InsightsLatestPostModel> {
        val response = latestPostStore.fetchLatestPostInsights(statsSiteProvider.siteModel, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(
                    error.message ?: error.type.name
            )
            model != null -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_latest_post_summary))

    override fun buildEmptyItem(): List<BlockListItem> {
        return buildNullableUiModel(null)
    }

    override fun buildUiModel(domainModel: InsightsLatestPostModel): List<BlockListItem> {
        return buildNullableUiModel(domainModel)
    }

    private fun buildNullableUiModel(domainModel: InsightsLatestPostModel?): MutableList<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_insights_latest_post_summary))
        items.add(latestPostSummaryMapper.buildMessageItem(domainModel, this::onLinkClicked))
        if (domainModel != null && domainModel.hasData()) {
            items.add(
                    ValueItem(
                            domainModel.postViewsCount.toFormattedString(startValue = HUNDRED_THOUSAND),
                            R.string.stats_views
                    )
            )
            if (domainModel.dayViews.isNotEmpty()) {
                items.add(latestPostSummaryMapper.buildBarChartItem(domainModel.dayViews))
            }
            items.add(
                    ListItemWithIcon(
                            R.drawable.ic_star_white_24dp,
                            textResource = R.string.stats_likes,
                            value = domainModel.postLikeCount.toFormattedString(),
                            showDivider = true
                    )
            )
            items.add(
                    ListItemWithIcon(
                            R.drawable.ic_comment_white_24dp,
                            textResource = R.string.stats_comments,
                            value = domainModel.postCommentCount.toFormattedString(),
                            showDivider = false
                    )
            )
        }
        items.add(buildLink(domainModel))
        return items
    }

    private fun InsightsLatestPostModel.hasData() =
            this.postViewsCount > 0 || this.postCommentCount > 0 || this.postLikeCount > 0

    private fun buildLink(model: InsightsLatestPostModel?): Link {
        return when {
            model == null -> Link(
                    R.drawable.ic_create_white_24dp,
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
                    R.drawable.ic_share_white_24dp,
                    R.string.stats_insights_share_post,
                    navigateAction = NavigationAction.create(
                            SharePostParams(model.postURL, model.postTitle),
                            this::onSharePost
                    )
            )
        }
    }

    private fun onAddNewPostClick() {
        analyticsTracker.track(STATS_LATEST_POST_SUMMARY_ADD_NEW_POST_TAPPED)
        navigateTo(AddNewPost())
    }

    private fun onViewMore(params: ViewMoreParams) {
        analyticsTracker.track(STATS_LATEST_POST_SUMMARY_VIEW_POST_DETAILS_TAPPED)
        navigateTo(
                ViewPostDetailStats(
                        params.postId,
                        params.postTitle,
                        params.postUrl
                )
        )
    }

    private fun onSharePost(params: SharePostParams) {
        analyticsTracker.track(STATS_LATEST_POST_SUMMARY_SHARE_POST_TAPPED)
        navigateTo(SharePost(params.postUrl, params.postTitle))
    }

    private fun onLinkClicked(params: LinkClickParams) {
        analyticsTracker.track(STATS_LATEST_POST_SUMMARY_POST_ITEM_TAPPED)
        navigateTo(ViewPost(params.postId, params.postUrl))
    }

    data class LinkClickParams(val postId: Long, val postUrl: String)
    data class SharePostParams(val postUrl: String, val postTitle: String)
    data class ViewMoreParams(val postId: Long, val postTitle: String, val postUrl: String)
}
