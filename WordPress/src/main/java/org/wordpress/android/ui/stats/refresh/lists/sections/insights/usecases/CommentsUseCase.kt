package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.fluxc.store.stats.insights.CommentsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

typealias SelectedTabUiState = Int

private const val BLOCK_ITEM_COUNT = 6

class CommentsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : StatefulUseCase<CommentsModel, SelectedTabUiState>(COMMENTS, mainDispatcher, 0) {
    override suspend fun fetchRemoteData(forced: Boolean): State<CommentsModel> {
        val fetchMode = if (useCaseMode == VIEW_ALL) LimitMode.All else LimitMode.Top(BLOCK_ITEM_COUNT)
        val response = commentsStore.fetchComments(statsSiteProvider.siteModel, fetchMode, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && (model.authors.isNotEmpty() || model.posts.isNotEmpty()) -> State.Data(
                    model,
                    cached = response.cached)
            else -> State.Empty()
        }
    }

    override suspend fun loadCachedData(): CommentsModel? {
        val cacheMode = if (useCaseMode == VIEW_ALL) LimitMode.All else LimitMode.Top(BLOCK_ITEM_COUNT)
        return commentsStore.getComments(statsSiteProvider.siteModel, cacheMode)
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_comments))

    override fun buildStatefulUiModel(model: CommentsModel, uiState: Int): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(string.stats_view_comments))
        }

        if (model.authors.isNotEmpty() || model.posts.isNotEmpty()) {
            items.add(
                    TabsItem(
                            listOf(R.string.stats_comments_authors, R.string.stats_comments_posts_and_pages),
                            uiState
                    ) { selectedTabPosition -> onUiState(selectedTabPosition) }
            )

            if (uiState == 0) {
                items.addAll(buildAuthorsTab(model.authors))
            } else {
                items.addAll(buildPostsTab(model.posts))
            }

            if (model.hasMoreAuthors && uiState == 0 || model.hasMorePosts && uiState == 1) {
                items.add(
                        Link(
                                text = string.stats_insights_view_more,
                                navigateAction = NavigationAction.create(uiState, this::onLinkClick)
                        )
                )
            }
        } else {
            items.add(Empty())
        }
        return items
    }

    private fun buildAuthorsTab(authors: List<CommentsModel.Author>): List<BlockListItem> {
        val mutableItems = mutableListOf<BlockListItem>()
        if (authors.isNotEmpty()) {
            mutableItems.add(Header(R.string.stats_comments_author_label, R.string.stats_comments_label))
            mutableItems.addAll(authors.mapIndexed { index, author ->
                ListItemWithIcon(
                        iconUrl = author.gravatar,
                        iconStyle = AVATAR,
                        text = author.name,
                        value = author.comments.toFormattedString(),
                        showDivider = index < authors.size - 1
                )
            })
        } else {
            mutableItems.add(Empty())
        }
        return mutableItems
    }

    private fun buildPostsTab(posts: List<CommentsModel.Post>): List<BlockListItem> {
        val mutableItems = mutableListOf<BlockListItem>()
        if (posts.isNotEmpty()) {
            mutableItems.add(Header(R.string.stats_comments_title_label, R.string.stats_comments_label))
            mutableItems.addAll(posts.mapIndexed { index, post ->
                ListItem(
                        post.name,
                        post.comments.toFormattedString(),
                        index < posts.size - 1
                )
            })
        } else {
            mutableItems.add(Empty())
        }
        return mutableItems
    }

    private fun onLinkClick(selectedTab: Int) {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_COMMENTS_VIEW_MORE_TAPPED)
        navigateTo(ViewCommentsStats(selectedTab))
    }

    class CommentsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val commentsStore: CommentsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                CommentsUseCase(
                        mainDispatcher,
                        commentsStore,
                        statsSiteProvider,
                        analyticsTracker,
                        useCaseMode
                )
    }
}
