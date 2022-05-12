package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.stats.insights.CommentsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6

class PostsCommentsUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper
) : StatelessUseCase<CommentsModel>(InsightType.POSTS_COMMENTS, mainDispatcher, backgroundDispatcher) {
    override suspend fun fetchRemoteData(forced: Boolean): State<CommentsModel> {
        val fetchMode = LimitMode.Top(BLOCK_ITEM_COUNT)
        val response = commentsStore.fetchComments(statsSiteProvider.siteModel, fetchMode, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && (model.posts.isNotEmpty()) -> State.Data(
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

    override fun buildLoadingItem(): List<BlockListItem> = listOf(buildTitle())

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override fun buildUiModel(domainModel: CommentsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (domainModel.posts.isNotEmpty()) {
            val header = Header(
                    R.string.posts,
                    R.string.stats_comments_label,
                    listOf(resourceProvider.getString(string.posts))
            )
            items.add(header)
            items.addAll(domainModel.posts.mapIndexed { index, post ->
                ListItem(
                        post.name,
                        statsUtils.toFormattedString(post.comments),
                        index < domainModel.posts.size - 1,
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                                header,
                                post.name,
                                post.comments
                        )
                )
            })
        } else {
            items.add(Empty())
        }
        return items
    }

    private fun buildTitle() = Title(string.posts, menuAction = null)
}
