package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.AUTHORS
import org.wordpress.android.fluxc.store.stats.time.AuthorsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewAuthors
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.EMPTY_SPACE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.AuthorsUseCase.SelectedAuthor
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.getBarWidth
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_ITEM_COUNT = 1000

class AuthorsUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val authorsStore: AuthorsStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val useCaseMode: UseCaseMode
) : GranularStatefulUseCase<AuthorsModel, SelectedAuthor>(
        AUTHORS,
        mainDispatcher,
        statsSiteProvider,
        selectedDateProvider,
        statsGranularity,
        SelectedAuthor()
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_authors))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): AuthorsModel? {
        return authorsStore.getAuthors(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<AuthorsModel> {
        val response = authorsStore.fetchAuthors(
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
            model != null && model.authors.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: AuthorsModel, uiState: SelectedAuthor): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_authors))
        }

        if (domainModel.authors.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            items.add(Header(R.string.stats_author_label, R.string.stats_author_views_label))
            val maxViews = domainModel.authors.maxBy { it.views }?.views ?: 0
            domainModel.authors.forEachIndexed { index, author ->
                val headerItem = ListItemWithIcon(
                        iconUrl = author.avatarUrl,
                        iconStyle = AVATAR,
                        text = author.name,
                        barWidth = getBarWidth(author.views, maxViews),
                        value = author.views.toFormattedString(),
                        showDivider = index < domainModel.authors.size - 1,
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                                R.string.stats_author_label,
                                author.name,
                                R.string.stats_author_views_label,
                                author.views
                        )
                )
                if (author.posts.isEmpty()) {
                    items.add(headerItem)
                } else {
                    val isExpanded = author == uiState.author
                    items.add(ExpandableItem(headerItem, isExpanded) { changedExpandedState ->
                        onUiState(SelectedAuthor(if (changedExpandedState) author else null))
                    })
                    if (isExpanded) {
                        items.addAll(author.posts.map { post ->
                            ListItemWithIcon(
                                    text = post.title,
                                    value = post.views.toFormattedString(),
                                    iconStyle = if (author.avatarUrl != null) EMPTY_SPACE else NORMAL,
                                    textStyle = LIGHT,
                                    showDivider = false,
                                    navigationAction = create(
                                            PostClickParams(post.id, post.url, post.title),
                                            this::onPostClicked
                                    ),
                                    contentDescription = contentDescriptionHelper.buildContentDescription(
                                            R.string.stats_post_label,
                                            post.title,
                                            R.string.stats_post_views_label,
                                            post.views
                                    )
                            )
                        })
                        items.add(Divider)
                    }
                }
            }

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
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_AUTHORS_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewAuthors(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
                )
        )
    }

    private fun onPostClicked(params: PostClickParams) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_AUTHORS_VIEW_POST_TAPPED, statsGranularity)
        navigateTo(
                ViewPostDetailStats(
                        postId = params.postId.toLong(),
                        postTitle = params.postTitle,
                        postUrl = params.postUrl,
                        postType = StatsConstants.ITEM_TYPE_POST
                )
        )
    }

    data class SelectedAuthor(val author: AuthorsModel.Author? = null)

    private data class PostClickParams(
        val postId: String,
        val postUrl: String?,
        val postTitle: String
    )

    class AuthorsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val authorsStore: AuthorsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val contentDescriptionHelper: ContentDescriptionHelper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                AuthorsUseCase(
                        granularity,
                        mainDispatcher,
                        authorsStore,
                        statsSiteProvider,
                        selectedDateProvider,
                        analyticsTracker,
                        contentDescriptionHelper,
                        useCaseMode
                )
    }
}
