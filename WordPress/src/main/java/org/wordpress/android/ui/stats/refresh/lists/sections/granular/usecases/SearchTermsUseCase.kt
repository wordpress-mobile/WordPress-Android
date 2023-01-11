package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.SEARCH_TERMS
import org.wordpress.android.fluxc.store.stats.time.SearchTermsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewSearchTerms
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.VIEW_ALL_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.ui.utils.ListItemInteraction.Companion.create
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
class SearchTermsUseCase constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val store: SearchTermsStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val useCaseMode: UseCaseMode
) : GranularStatelessUseCase<SearchTermsModel>(
    SEARCH_TERMS,
    mainDispatcher,
    backgroundDispatcher,
    selectedDateProvider,
    statsSiteProvider,
    statsGranularity
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_search_terms))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): SearchTermsModel? {
        return store.getSearchTerms(
            site,
            statsGranularity,
            LimitMode.Top(itemsToLoad),
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
            statsGranularity,
            LimitMode.Top(itemsToLoad),
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

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_search_terms))
        }

        if (domainModel.searchTerms.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            val header = Header(R.string.stats_search_terms_label, R.string.stats_search_terms_views_label)
            items.add(header)
            val hasEncryptedCount = domainModel.unknownSearchCount > 0
            val mappedSearchTerms = domainModel.searchTerms.mapIndexed { index, searchTerm ->
                ListItemWithIcon(
                    text = searchTerm.text,
                    value = statsUtils.toFormattedString(searchTerm.views),
                    showDivider = index < domainModel.searchTerms.size - 1,
                    contentDescription = contentDescriptionHelper.buildContentDescription(
                        header,
                        searchTerm.text,
                        searchTerm.views
                    )
                )
            }
            if (hasEncryptedCount) {
                items.addAll(mappedSearchTerms.take(mappedSearchTerms.size - 1))
                items.add(
                    ListItemWithIcon(
                        textResource = R.string.stats_search_terms_unknown_search_terms,
                        value = statsUtils.toFormattedString(domainModel.unknownSearchCount),
                        showDivider = false,
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                            header,
                            R.string.stats_search_terms_unknown_search_terms,
                            domainModel.unknownSearchCount
                        )
                    )
                )
            } else {
                items.addAll(mappedSearchTerms)
            }

            if (useCaseMode == BLOCK && domainModel.hasMore) {
                items.add(
                    Link(
                        text = R.string.stats_insights_view_more,
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
                selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
            )
        )
    }

    class SearchTermsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val store: SearchTermsStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsSiteProvider: StatsSiteProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsUtils: StatsUtils,
        private val contentDescriptionHelper: ContentDescriptionHelper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
            SearchTermsUseCase(
                granularity,
                mainDispatcher,
                backgroundDispatcher,
                store,
                statsSiteProvider,
                selectedDateProvider,
                analyticsTracker,
                contentDescriptionHelper,
                statsUtils,
                useCaseMode
            )
    }
}
