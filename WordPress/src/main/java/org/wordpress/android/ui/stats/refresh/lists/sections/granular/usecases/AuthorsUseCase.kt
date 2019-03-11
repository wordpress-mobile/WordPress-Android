package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.AUTHORS
import org.wordpress.android.fluxc.store.stats.time.AuthorsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewAuthors
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostDetailStats
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
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.AuthorsUseCase.SelectedAuthor
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class AuthorsUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val authorsStore: AuthorsStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : GranularStatefulUseCase<AuthorsModel, SelectedAuthor>(
        AUTHORS,
        mainDispatcher,
        statsSiteProvider,
        selectedDateProvider,
        statsGranularity,
        SelectedAuthor()
) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_authors))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): AuthorsModel? {
        return authorsStore.getAuthors(
                site,
                statsGranularity,
                PAGE_SIZE,
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<AuthorsModel> {
        val response = authorsStore.fetchAuthors(
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
            model != null && model.authors.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildStatefulUiModel(domainModel: AuthorsModel, uiState: SelectedAuthor): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_authors))

        if (domainModel.authors.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            items.add(Header(R.string.stats_author_label, R.string.stats_author_views_label))
            domainModel.authors.forEachIndexed { index, author ->
                val headerItem = ListItemWithIcon(
                        iconUrl = author.avatarUrl,
                        iconStyle = AVATAR,
                        text = author.name,
                        value = author.views.toFormattedString(),
                        showDivider = index < domainModel.authors.size - 1
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
                                    )
                            )
                        })
                        items.add(Divider)
                    }
                }
            }

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
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_AUTHORS_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewAuthors(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date(),
                        statsSiteProvider.siteModel
                )
        )
    }

    private fun onPostClicked(params: PostClickParams) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_AUTHORS_VIEW_POST_TAPPED, statsGranularity)
        navigateTo(
                ViewPostDetailStats(
                        postId = params.postId,
                        postTitle = params.postTitle,
                        postUrl = params.postUrl,
                        postType = StatsConstants.ITEM_TYPE_POST,
                        siteId = statsSiteProvider.siteModel.siteId
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
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                AuthorsUseCase(
                        granularity,
                        mainDispatcher,
                        authorsStore,
                        statsSiteProvider,
                        selectedDateProvider,
                        analyticsTracker
                )
    }
}
