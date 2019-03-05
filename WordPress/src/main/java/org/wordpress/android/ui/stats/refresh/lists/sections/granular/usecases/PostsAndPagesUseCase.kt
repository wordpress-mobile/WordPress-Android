package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.HOMEPAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.PAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.POST
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.POSTS_AND_PAGES
import org.wordpress.android.fluxc.store.stats.time.PostAndPageViewsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsConstants.ITEM_TYPE_HOME_PAGE
import org.wordpress.android.ui.stats.StatsConstants.ITEM_TYPE_POST
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class PostsAndPagesUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val postsAndPageViewsStore: PostAndPageViewsStore,
    selectedDateProvider: SelectedDateProvider,
    statsSiteProvider: StatsSiteProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : GranularStatelessUseCase<PostAndPageViewsModel>(
        POSTS_AND_PAGES,
        mainDispatcher,
        selectedDateProvider,
        statsSiteProvider,
        statsGranularity
) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_posts_and_pages))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): PostAndPageViewsModel? {
        return postsAndPageViewsStore.getPostAndPageViews(
                site,
                statsGranularity,
                selectedDate,
                PAGE_SIZE
        )
    }

    override suspend fun fetchRemoteData(
        selectedDate: Date,
        site: SiteModel,
        forced: Boolean
    ): State<PostAndPageViewsModel> {
        val response = postsAndPageViewsStore.fetchPostAndPageViews(
                site,
                PAGE_SIZE,
                statsGranularity,
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
        items.add(Title(string.stats_posts_and_pages))

        if (domainModel.views.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            items.add(Header(R.string.stats_posts_and_pages_title_label, R.string.stats_posts_and_pages_views_label))
            items.addAll(domainModel.views.mapIndexed { index, viewsModel ->
                val icon = when (viewsModel.type) {
                    POST -> R.drawable.ic_posts_white_24dp
                    HOMEPAGE, PAGE -> R.drawable.ic_pages_white_24dp
                }
                ListItemWithIcon(
                        icon = icon,
                        text = viewsModel.title,
                        value = viewsModel.views.toFormattedString(),
                        showDivider = index < domainModel.views.size - 1,
                        navigationAction = create(
                                LinkClickParams(viewsModel.id, viewsModel.url, viewsModel.title, viewsModel.type),
                                this::onLinkClicked
                        )
                )
            })
            if (domainModel.hasMore) {
                items.add(
                        Link(
                                text = string.stats_insights_view_more,
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
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date(),
                        statsSiteProvider.siteModel
                )
        )
    }

    private fun onLinkClicked(params: LinkClickParams) {
        val type = when (params.postType) {
            POST -> ITEM_TYPE_POST
            PAGE, HOMEPAGE -> ITEM_TYPE_HOME_PAGE
        }
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_POSTS_AND_PAGES_ITEM_TAPPED, statsGranularity)
        navigateTo(
                ViewPostDetailStats(
                        postId = params.postId.toString(),
                        postTitle = params.postTitle,
                        postUrl = params.postUrl,
                        postType = type,
                        siteId = statsSiteProvider.siteModel.siteId
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
        private val postsAndPageViewsStore: PostAndPageViewsStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsSiteProvider: StatsSiteProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                PostsAndPagesUseCase(
                        granularity,
                        mainDispatcher,
                        postsAndPageViewsStore,
                        selectedDateProvider,
                        statsSiteProvider,
                        analyticsTracker
                )
    }
}
