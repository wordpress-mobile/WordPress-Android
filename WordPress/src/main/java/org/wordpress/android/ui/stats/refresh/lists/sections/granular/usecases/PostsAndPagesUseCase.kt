package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
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
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class PostsAndPagesUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val postsAndPageViewsStore: PostAndPageViewsStore
) : StatelessUseCase<PostAndPageViewsModel>(POSTS_AND_PAGES, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = postsAndPageViewsStore.getPostAndPageViews(site, statsGranularity, PAGE_SIZE)
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = postsAndPageViewsStore.fetchPostAndPageViews(site, PAGE_SIZE, statsGranularity, forced)
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildUiModel(domainModel: PostAndPageViewsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_posts_and_pages))

        if (domainModel.views.isEmpty()) {
            items.add(Empty)
        } else {
            items.addAll(domainModel.views.mapIndexed { index, viewsModel ->
                val icon = when (viewsModel.type) {
                    POST -> R.drawable.ic_posts_grey_dark_24dp
                    HOMEPAGE, PAGE -> R.drawable.ic_pages_grey_dark_24dp
                }
                ListItemWithIcon(
                        icon = icon,
                        text = viewsModel.title,
                        value = viewsModel.views.toFormattedString(),
                        showDivider = index < domainModel.views.size - 1,
                        navigationAction = create(
                                LinkClickParams(viewsModel.id, viewsModel.url, viewsModel.type),
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
        navigateTo(ViewPostsAndPages(statsGranularity))
    }

    private fun onLinkClicked(params: LinkClickParams) {
        val type = when (params.postType) {
            POST -> ITEM_TYPE_POST
            PAGE, HOMEPAGE -> ITEM_TYPE_HOME_PAGE
        }
        navigateTo(ViewPost(params.postId, params.postUrl, type))
    }

    private data class LinkClickParams(
        val postId: Long,
        val postUrl: String,
        val postType: PostAndPageViewsModel.ViewsType
    )

    class PostsAndPagesUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val postsAndPageViewsStore: PostAndPageViewsStore
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                PostsAndPagesUseCase(granularity, mainDispatcher, postsAndPageViewsStore)
    }
}
