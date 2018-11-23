package org.wordpress.android.ui.stats.refresh.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.BlockListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.UserItem
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.ListInsightItem.ListUiState
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class CommentsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore
) : BaseInsightsUseCase(COMMENTS, mainDispatcher) {
    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): InsightsItem? {
        val response = insightsStore.fetchComments(site, PAGE_SIZE, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> createFailedItem(
                    string.stats_view_comments,
                    error.message ?: error.type.name
            )
            else -> model?.let { loadComments(site, model) }
        }
    }

    override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
        val dbModel = insightsStore.getComments(site, PAGE_SIZE)
        return dbModel?.let { loadComments(site, dbModel) }
    }

    private fun loadComments(
        site: SiteModel,
        model: CommentsModel,
        uiState: CommentsUiState = commentsUiState
    ): InsightsItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_comments))

        val selectedTabPosition = uiState.tabSelectedPosition
        items.add(
                TabsItem(
                        listOf(R.string.stats_comments_authors, R.string.stats_comments_posts_and_pages),
                        selectedTabPosition
                ) {
                    onDataChanged(loadComments(site, model, commentsUiState.copy(tabSelectedPosition = it)))
                })

        if (selectedTabPosition == 0) {
            items.addAll(buildAuthorsTab(model.authors))
        } else {
            items.addAll(buildPostsTab(model.posts))
        }

        if (model.hasMoreAuthors || model.hasMorePosts) {
            items.add(Link(text = string.stats_insights_view_more) {
                navigateTo(ViewCommentsStats(site.siteId))
            })
        }
        return createDataItem(items, uiState)
    }

    private fun buildAuthorsTab(authors: List<CommentsModel.Author>): List<BlockListItem> {
        val mutableItems = mutableListOf<BlockListItem>()
        if (authors.isNotEmpty()) {
            mutableItems.add(Label(R.string.stats_comments_author_label, R.string.stats_comments_label))
            mutableItems.addAll(authors.take(PAGE_SIZE).mapIndexed { index, author ->
                UserItem(
                        author.gravatar,
                        author.name,
                        author.comments.toFormattedString(),
                        index < authors.size - 1
                )
            })
        } else {
            mutableItems.add(Empty)
        }
        return mutableItems
    }

    private fun buildPostsTab(posts: List<CommentsModel.Post>): List<BlockListItem> {
        val mutableItems = mutableListOf<BlockListItem>()
        if (posts.isNotEmpty()) {
            mutableItems.add(Label(R.string.stats_comments_title_label, R.string.stats_comments_label))
            mutableItems.addAll(posts.take(PAGE_SIZE).mapIndexed { index, post ->
                ListItem(
                        post.name,
                        post.comments.toFormattedString(),
                        index < posts.size - 1
                )
            })
        } else {
            mutableItems.add(Empty)
        }
        return mutableItems
    }

    private val commentsUiState
        get() = uiState as? CommentsUiState ?: CommentsUiState(0)

    data class CommentsUiState(val tabSelectedPosition: Int) : ListUiState
}
