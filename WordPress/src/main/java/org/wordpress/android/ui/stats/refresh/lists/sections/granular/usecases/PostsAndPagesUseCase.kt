package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.ATTACHMENT
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.HOMEPAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.OTHER
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.PAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.POST
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.POSTS_AND_PAGES
import org.wordpress.android.fluxc.store.stats.time.PostAndPageViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsConstants.ITEM_TYPE_ATTACHMENT
import org.wordpress.android.ui.stats.StatsConstants.ITEM_TYPE_HOME_PAGE
import org.wordpress.android.ui.stats.StatsConstants.ITEM_TYPE_POST
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.VIEW_ALL_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.getBarWidth
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.ui.utils.ListItemInteraction.Companion.create
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class PostsAndPagesUseCase
@Inject constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val postsAndPageViewsStore: PostAndPageViewsStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val useCaseMode: UseCaseMode
) : GranularStatelessUseCase<PostAndPageViewsModel>(
        POSTS_AND_PAGES,
        mainDispatcher,
        backgroundDispatcher,
        selectedDateProvider,
        statsSiteProvider,
        statsGranularity
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_posts_and_pages))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): PostAndPageViewsModel? {
        return postsAndPageViewsStore.getPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(
        selectedDate: Date,
        site: SiteModel,
        forced: Boolean
    ): State<PostAndPageViewsModel> {
        val response = postsAndPageViewsStore.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate,
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.views.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: PostAndPageViewsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK || useCaseMode == BLOCK_DETAIL) {
            items.add(Title(R.string.stats_posts_and_pages))
        }

        if (domainModel.views.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            val header = Header(R.string.stats_posts_and_pages_title_label, R.string.stats_posts_and_pages_views_label)
            items.add(header)
            val maxViews = domainModel.views.maxByOrNull { it.views }?.views ?: 0
            items.addAll(domainModel.views.mapIndexed { index, viewsModel ->
                val icon = when (viewsModel.type) {
                    POST -> R.drawable.ic_posts_white_24dp
                    OTHER, HOMEPAGE, PAGE, ATTACHMENT -> R.drawable.ic_pages_white_24dp
                }
                ListItemWithIcon(
                        icon = icon,
                        text = viewsModel.title,
                        value = statsUtils.toFormattedString(viewsModel.views),
                        showDivider = index < domainModel.views.size - 1,
                        barWidth = getBarWidth(viewsModel.views, maxViews),
                        navigationAction = create(
                                LinkClickParams(viewsModel.id, viewsModel.url, viewsModel.title, viewsModel.type),
                                this::onLinkClicked
                        ),
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                                header,
                                viewsModel.title,
                                viewsModel.views
                        )
                )
            })
            if (useCaseMode == BLOCK && domainModel.hasMore) {
                items.add(
                        Link(
                                text = R.string.stats_insights_view_more,
                                navigateAction = create(statsGranularity, this::onViewMoreClicked)
                        )
                )
            }
        }
        return items
    }

    private fun onViewMoreClicked(statsGranularity: StatsGranularity) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_POSTS_AND_PAGES_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewPostsAndPages(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
                )
        )
    }

    private fun onLinkClicked(params: LinkClickParams) {
        val type = when (params.postType) {
            POST -> ITEM_TYPE_POST
            OTHER, PAGE, HOMEPAGE -> ITEM_TYPE_HOME_PAGE
            ATTACHMENT -> ITEM_TYPE_ATTACHMENT
        }
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_POSTS_AND_PAGES_ITEM_TAPPED, statsGranularity)
        navigateTo(
                ViewPostDetailStats(
                        postId = params.postId,
                        postTitle = params.postTitle,
                        postUrl = params.postUrl,
                        postType = type
                )
        )
    }

    private data class LinkClickParams(
        val postId: Long,
        val postUrl: String,
        val postTitle: String,
        val postType: PostAndPageViewsModel.ViewsType
    )

    class PostsAndPagesUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val postsAndPageViewsStore: PostAndPageViewsStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsSiteProvider: StatsSiteProvider,
        private val contentDescriptionHelper: ContentDescriptionHelper,
        private val statsUtils: StatsUtils,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                PostsAndPagesUseCase(
                        granularity,
                        mainDispatcher,
                        backgroundDispatcher,
                        postsAndPageViewsStore,
                        statsSiteProvider,
                        selectedDateProvider,
                        analyticsTracker,
                        contentDescriptionHelper,
                        statsUtils,
                        useCaseMode
                )
    }
}
