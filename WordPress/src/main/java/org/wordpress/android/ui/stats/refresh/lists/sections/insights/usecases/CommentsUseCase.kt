package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.store.StatsStore.InsightType.COMMENTS
import org.wordpress.android.fluxc.store.stats.insights.CommentsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject
import javax.inject.Named

typealias SelectedTabUiState = Int

private const val BLOCK_ITEM_COUNT = 6

class CommentsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val popupMenuHandler: ItemPopupMenuHandler,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper
) : BaseStatsUseCase<CommentsModel, SelectedTabUiState>(COMMENTS, mainDispatcher, backgroundDispatcher, 0) {
    override suspend fun fetchRemoteData(forced: Boolean): State<CommentsModel> {
        val fetchMode = LimitMode.Top(BLOCK_ITEM_COUNT)
        val response = commentsStore.fetchComments(statsSiteProvider.siteModel, fetchMode, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && (model.authors.isNotEmpty() || model.posts.isNotEmpty()) -> State.Data(
                    model,
                    cached = response.cached
            )
            else -> State.Empty()
        }
    }

    override suspend fun loadCachedData(): CommentsModel? {
        val cacheMode = LimitMode.Top(BLOCK_ITEM_COUNT)
        return commentsStore.getComments(statsSiteProvider.siteModel, cacheMode)
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_comments))

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override fun buildUiModel(domainModel: CommentsModel, uiState: Int): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())
        if (domainModel.authors.isNotEmpty() || domainModel.posts.isNotEmpty()) {
            items.add(
                    TabsItem(
                            listOf(R.string.stats_comments_authors, R.string.stats_comments_posts_and_pages),
                            uiState
                    ) { selectedTabPosition -> onUiState(selectedTabPosition) }
            )

            if (uiState == 0) {
                items.addAll(buildAuthorsTab(domainModel.authors))
            } else {
                items.addAll(buildPostsTab(domainModel.posts))
            }
        } else {
            items.add(Empty())
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_view_comments, menuAction = this::onMenuClick)

    private fun buildAuthorsTab(authors: List<CommentsModel.Author>): List<BlockListItem> {
        val mutableItems = mutableListOf<BlockListItem>()
        if (authors.isNotEmpty()) {
            val header = Header(R.string.stats_comments_author_label, R.string.stats_comments_label)
            mutableItems.add(header)
            mutableItems.addAll(authors.mapIndexed { index, author ->
                ListItemWithIcon(
                        iconUrl = author.gravatar,
                        iconStyle = AVATAR,
                        text = author.name,
                        value = statsUtils.toFormattedString(author.comments),
                        showDivider = index < authors.size - 1,
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                                header,
                                author.name,
                                author.comments
                        )
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
            val header = Header(R.string.stats_comments_title_label, R.string.stats_comments_label)
            mutableItems.add(header)
            mutableItems.addAll(posts.mapIndexed { index, post ->
                ListItem(
                        post.name,
                        statsUtils.toFormattedString(post.comments),
                        index < posts.size - 1,
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                                header,
                                post.name,
                                post.comments
                        )
                )
            })
        } else {
            mutableItems.add(Empty())
        }
        return mutableItems
    }

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }
}
