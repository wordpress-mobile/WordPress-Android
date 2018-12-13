package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

typealias SelectedTabUiState = Int

private const val PAGE_SIZE = 6

class CommentsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore
) : StatefulUseCase<CommentsModel, SelectedTabUiState>(COMMENTS, mainDispatcher, 0) {
    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = insightsStore.fetchComments(site, PAGE_SIZE, forced)
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = insightsStore.getComments(site, PAGE_SIZE)
        dbModel?.let { onModel(dbModel) }
    }

    override fun buildStatefulUiModel(model: CommentsModel, uiState: Int): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_comments))

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

        if (model.hasMoreAuthors || model.hasMorePosts) {
            items.add(
                    Link(
                            text = string.stats_insights_view_more,
                            navigateAction = NavigationAction.create(this::onLinkClick)
                    )
            )
        }
        return items
    }

    private fun buildAuthorsTab(authors: List<CommentsModel.Author>): List<BlockListItem> {
        val mutableItems = mutableListOf<BlockListItem>()
        if (authors.isNotEmpty()) {
            mutableItems.add(Label(R.string.stats_comments_author_label, R.string.stats_comments_label))
            mutableItems.addAll(authors.take(PAGE_SIZE).mapIndexed { index, author ->
                ListItemWithIcon(
                        iconUrl = author.gravatar,
                        iconStyle = AVATAR,
                        text = author.name,
                        value = author.comments.toFormattedString(),
                        showDivider = index < authors.size - 1
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

    private fun onLinkClick() {
        navigateTo(ViewCommentsStats())
    }
}
