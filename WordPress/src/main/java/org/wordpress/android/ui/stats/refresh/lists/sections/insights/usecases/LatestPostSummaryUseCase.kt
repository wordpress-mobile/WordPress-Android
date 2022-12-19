package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_ADD_NEW_POST_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_POST_ITEM_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_SHARE_POST_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_LATEST_POST_SUMMARY_VIEW_POST_DETAILS_TAPPED
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.stats.insights.LatestPostInsightsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class LatestPostSummaryUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val latestPostStore: LatestPostInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val latestPostSummaryMapper: LatestPostSummaryMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val popupMenuHandler: ItemPopupMenuHandler,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper
) : StatelessUseCase<InsightsLatestPostModel>(LATEST_POST_SUMMARY, mainDispatcher, backgroundDispatcher) {
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

        if (BuildConfig.IS_JETPACK_APP) {
            items.add(buildTitleViewMore(domainModel))
            items.add(latestPostSummaryMapper.buildLatestPostItem(domainModel))
            if (domainModel != null && domainModel.hasData()) items.add(buildQuickScanItems(domainModel))
        } else {
            items.add(buildTitle())
            items.add(latestPostSummaryMapper.buildMessageItem(domainModel, this::onLinkClicked))
            if (domainModel != null && domainModel.hasData()) {
                items.add(
                        ValueItem(
                                statsUtils.toFormattedString(domainModel.postViewsCount, startValue = MILLION),
                                R.string.stats_views,
                                contentDescription = contentDescriptionHelper.buildContentDescription(
                                        R.string.stats_views,
                                        domainModel.postViewsCount
                                )
                        )
                )
                if (domainModel.dayViews.isNotEmpty()) {
                    items.add(latestPostSummaryMapper.buildBarChartItem(domainModel.dayViews))
                }
                val postLikeCount = statsUtils.toFormattedString(domainModel.postLikeCount)
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_star_white_24dp,
                                textResource = R.string.stats_likes,
                                value = postLikeCount,
                                showDivider = true,
                                contentDescription = contentDescriptionHelper.buildContentDescription(
                                        R.string.stats_likes,
                                        domainModel.postLikeCount
                                )
                        )
                )
                val postCommentCount = statsUtils.toFormattedString(domainModel.postCommentCount)
                items.add(
                        ListItemWithIcon(
                                R.drawable.ic_comment_white_24dp,
                                textResource = R.string.stats_comments,
                                value = postCommentCount,
                                showDivider = false,
                                contentDescription = contentDescriptionHelper.buildContentDescription(
                                        R.string.stats_comments,
                                        domainModel.postCommentCount
                                )
                        )
                )
            }
        }
        buildLink(domainModel)?.let { items.add(it) }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_insights_latest_post_summary, menuAction = this::onMenuClick)

    private fun buildTitleViewMore(model: InsightsLatestPostModel?) = TitleWithMore(
            textResource = R.string.stats_insights_latest_post_summary,
            navigationAction = if (model?.hasData() == true) {
                ListItemInteraction.create(
                        ViewMoreParams(model.postId, model.postTitle, model.postURL),
                        this::onViewMore
                )
            } else {
                null
            }
    )

    private fun buildQuickScanItems(domainModel: InsightsLatestPostModel) =
        QuickScanItem(
                Column(
                        label = string.stats_views,
                        value = statsUtils.toFormattedString(domainModel.postViewsCount, startValue = MILLION),
                        tooltip = contentDescriptionHelper.buildContentDescription(
                                R.string.stats_views,
                                domainModel.postViewsCount
                        )
                ),
                Column(
                        label = string.stats_likes,
                        value = statsUtils.toFormattedString(domainModel.postLikeCount),
                        tooltip = contentDescriptionHelper.buildContentDescription(
                                R.string.stats_likes,
                                domainModel.postLikeCount
                        )
                ),
                Column(
                        label = string.stats_comments,
                        value = statsUtils.toFormattedString(domainModel.postCommentCount),
                        tooltip = contentDescriptionHelper.buildContentDescription(
                                R.string.stats_comments,
                                domainModel.postCommentCount
                        )
                )
        )

    private fun InsightsLatestPostModel.hasData() =
            this.postViewsCount > 0 || this.postCommentCount > 0 || this.postLikeCount > 0

    private fun buildLink(model: InsightsLatestPostModel?): Link? {
        return when {
            model == null -> Link(
                    R.drawable.ic_create_white_24dp,
                    R.string.stats_insights_create_post,
                    navigateAction = ListItemInteraction.create(this::onAddNewPostClick)
            )
            model.hasData() -> {
                if (!BuildConfig.IS_JETPACK_APP) {
                    Link(
                            text = R.string.stats_insights_view_more,
                            navigateAction = ListItemInteraction.create(
                                    ViewMoreParams(model.postId, model.postTitle, model.postURL),
                                    this::onViewMore
                            )
                    )
                } else {
                    null
                }
            }
            else -> Link(
                    R.drawable.ic_share_white_24dp,
                    R.string.stats_insights_share_post,
                    navigateAction = ListItemInteraction.create(
                            SharePostParams(model.postURL, model.postTitle),
                            this::onSharePost
                    )
            )
        }
    }

    private fun onAddNewPostClick() {
        analyticsTracker.track(STATS_LATEST_POST_SUMMARY_ADD_NEW_POST_TAPPED)
        navigateTo(AddNewPost)
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

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }

    data class LinkClickParams(val postId: Long, val postUrl: String)
    data class SharePostParams(val postUrl: String, val postTitle: String)
    data class ViewMoreParams(val postId: Long, val postTitle: String, val postUrl: String)
}
