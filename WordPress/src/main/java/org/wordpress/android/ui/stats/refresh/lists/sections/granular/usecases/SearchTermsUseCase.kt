package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.SEARCH_TERMS
import org.wordpress.android.fluxc.store.stats.time.SearchTermsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewSearchTerms
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class SearchTermsUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: SearchTermsStore,
    selectedDateProvider: SelectedDateProvider,
    statsSiteProvider: StatsSiteProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : GranularStatelessUseCase<SearchTermsModel>(
        SEARCH_TERMS,
        mainDispatcher,
        selectedDateProvider,
        statsSiteProvider,
        statsGranularity
) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_search_terms))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): SearchTermsModel? {
        return store.getSearchTerms(
                site,
                statsGranularity,
                PAGE_SIZE,
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(
        selectedDate: Date,
        site: SiteModel,
        forced: Boolean
    ): State<SearchTermsModel> {
        val response = store.fetchSearchTerms(
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
            model != null && model.searchTerms.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: SearchTermsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_search_terms))

        if (domainModel.searchTerms.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            items.add(Header(string.stats_search_terms_label, string.stats_search_terms_views_label))
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
                items.add(
                        ListItemWithIcon(
                                textResource = R.string.stats_search_terms_unknown_search_terms,
                                value = domainModel.unknownSearchCount.toFormattedString(),
                                showDivider = false
                        )
                )
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
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_SEARCH_TERMS_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewSearchTerms(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date(),
                        statsSiteProvider.siteModel
                )
        )
    }

    class SearchTermsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val store: SearchTermsStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsSiteProvider: StatsSiteProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                SearchTermsUseCase(
                        granularity,
                        mainDispatcher,
                        store,
                        selectedDateProvider,
                        statsSiteProvider,
                        analyticsTracker
                )
    }
}
