package org.wordpress.android.ui.stats.refresh.lists.sections.dwmy.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.PAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.POST
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.stats.time.PostAndPageViewsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class PostsAndPagesUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val postsAndPageViewsStore: PostAndPageViewsStore
) : BaseStatsUseCase(ALL_TIME_STATS, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel): StatsBlock? {
        val dbModel = postsAndPageViewsStore.getPostAndPageViews(site, statsGranularity, PAGE_SIZE)
        return dbModel?.let { loadPostsAndPages(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): StatsBlock? {
        val response = postsAndPageViewsStore.fetchPostAndPageViews(site, PAGE_SIZE, statsGranularity, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> createFailedItem(R.string.stats_insights_all_time_stats, error.message ?: error.type.name)
            else -> model?.let { loadPostsAndPages(model) }
        }
    }

    private fun loadPostsAndPages(model: PostAndPageViewsModel): StatsBlock {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_insights_all_time_stats))

        if (model.views.isEmpty()) {
            items.add(Empty)
        } else {
            items.addAll(model.views.mapIndexed { index, viewsModel ->
                val icon = when (viewsModel.type) {
                    POST -> R.drawable.ic_posts_grey_dark_24dp
                    PAGE -> R.drawable.ic_pages_grey_dark_24dp
                }
                ListItemWithIcon(
                        icon = icon,
                        text = viewsModel.title,
                        value = viewsModel.views.toFormattedString(),
                        showDivider = index < model.views.size - 1
                )
            })
            if (model.hasMore) {
                items.add(Link(text = string.stats_insights_view_more) {
                    navigateTo(ViewPostsAndPages(statsGranularity))
                })
            }
        }
        return createDataItem(items)
    }

    class PostsAndPagesUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val postsAndPageViewsStore: PostAndPageViewsStore
    ) {
        fun build(statsGranularity: StatsGranularity) =
                PostsAndPagesUseCase(statsGranularity, mainDispatcher, postsAndPageViewsStore)
    }
}
