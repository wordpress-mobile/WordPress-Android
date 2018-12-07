package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.AUTHORS
import org.wordpress.android.fluxc.store.stats.time.AuthorsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.PostListAction
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.CIRCLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.AuthorsUseCase.SelectedAuthor
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class AuthorsUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val authorsStore: AuthorsStore,
    private val statsDateFormatter: StatsDateFormatter
) : StatefulUseCase<AuthorsModel, SelectedAuthor>(AUTHORS, mainDispatcher, SelectedAuthor()) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = authorsStore.getAuthors(site, statsGranularity,
                PAGE_SIZE
        )
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = authorsStore.fetchAuthors(site,
                PAGE_SIZE, statsGranularity, forced)
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildStatefulUiModel(domainModel: AuthorsModel, uiState: SelectedAuthor): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_referrers))

        if (domainModel.authors.isEmpty()) {
            items.add(Empty)
        } else {
            items.add(Label(R.string.stats_referrer_label, R.string.stats_referrer_views_label))
            domainModel.authors.forEachIndexed { index, author ->
                val headerItem = ListItemWithIcon(
                        iconUrl = author.avatar,
                        iconStyle = CIRCLE,
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
                                    showDivider = false,
                                    navigationAction = create(post.url, )
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
        navigateTo(ViewReferrers(statsGranularity, statsDateFormatter.todaysDateInStatsFormat()))
    }

    private fun onPostClicked(url: String) {
        navigationTarget(View())
    }

    data class SelectedAuthor(val author: AuthorsModel.Author? = null)

    class AuthorsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val authorsStore: AuthorsStore,
        private val statsDateFormatter: StatsDateFormatter
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                AuthorsUseCase(
                        granularity,
                        mainDispatcher,
                        authorsStore,
                        statsDateFormatter
                )
    }
}
