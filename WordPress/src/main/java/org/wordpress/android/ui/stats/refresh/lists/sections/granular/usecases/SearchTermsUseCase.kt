package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.SEARCH_TERMS
import org.wordpress.android.fluxc.store.stats.time.SearchTermsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewSearchTerms
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class SearchTermsUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: SearchTermsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter
) : StatelessUseCase<SearchTermsModel>(SEARCH_TERMS, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = store.getSearchTerms(
                site,
                statsGranularity,
                PAGE_SIZE,
                selectedDateProvider.getSelectedDate(statsGranularity)
        )
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = store.fetchSearchTerms(
                site,
                PAGE_SIZE,
                statsGranularity,
                selectedDateProvider.getSelectedDate(statsGranularity),
                forced
        )
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildUiModel(domainModel: SearchTermsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_search_terms))

        if (domainModel.searchTerms.isEmpty()) {
            items.add(Empty)
        } else {
            items.add(Label(R.string.stats_search_terms_label, R.string.stats_search_terms_views_label))
            val hasEncryptedCount = domainModel.unknownSearchCount > 0
            val mappedSearchTerms = domainModel.searchTerms.mapIndexed { index, searchTerm ->
                ListItemWithIcon(
                        text = searchTerm.text,
                        value = searchTerm.views.toFormattedString(),
                        showDivider = index < domainModel.searchTerms.size - 1
                )
            }
            if (hasEncryptedCount) {
                items.addAll(mappedSearchTerms.take(mappedSearchTerms.size - 1))
                items.add(ListItemWithIcon(
                        textResource = R.string.stats_search_terms_unknown_search_terms,
                        value = domainModel.unknownSearchCount.toFormattedString(),
                        showDivider = false
                ))
            } else {
                items.addAll(mappedSearchTerms)
            }

            if (domainModel.hasMore) {
                items.add(
                        Link(
                                text = string.stats_insights_view_more,
                                navigateAction = create(statsGranularity, this::onViewMoreClick)
                        )
                )
            }
        }
        return items
    }

    private fun onViewMoreClick(statsGranularity: StatsGranularity) {
        navigateTo(ViewSearchTerms(statsGranularity, statsDateFormatter.todaysDateInStatsFormat()))
    }

    class SearchTermsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val store: SearchTermsStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                SearchTermsUseCase(
                        granularity,
                        mainDispatcher,
                        store,
                        selectedDateProvider,
                        statsDateFormatter
                )
    }
}
