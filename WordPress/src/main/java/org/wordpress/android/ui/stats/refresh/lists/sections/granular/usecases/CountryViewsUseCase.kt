package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.CountryViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.COUNTRIES
import org.wordpress.android.fluxc.store.stats.time.CountryViewsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCountries
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_ITEM_COUNT = 1000

class CountryViewsUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: CountryViewsStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : GranularStatelessUseCase<CountryViewsModel>(
        COUNTRIES,
        mainDispatcher,
        selectedDateProvider,
        statsSiteProvider,
        statsGranularity
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_countries))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): CountryViewsModel? {
        return store.getCountryViews(
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
    ): State<CountryViewsModel> {
        val response = store.fetchCountryViews(
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
            model != null && model.countries.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: CountryViewsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_countries))
        }

        if (domainModel.countries.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            val stringBuilder = StringBuilder()
            for (country in domainModel.countries) {
                stringBuilder.append("['").append(country.countryCode).append("',").append(country.views).append("],")
            }
            items.add(MapItem(stringBuilder.toString(), R.string.stats_country_views_label))
            items.add(Header(string.stats_country_label, string.stats_country_views_label))
            domainModel.countries.forEachIndexed { index, group ->
                items.add(
                        ListItemWithIcon(
                                iconUrl = group.flagIconUrl,
                                text = group.fullName,
                                value = group.views.toFormattedString(),
                                showDivider = index < domainModel.countries.size - 1
                        )
                )
            }

            if (useCaseMode == BLOCK && domainModel.hasMore) {
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
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_COUNTRIES_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewCountries(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
                )
        )
    }

    class CountryViewsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val store: CountryViewsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                CountryViewsUseCase(
                        granularity,
                        mainDispatcher,
                        store,
                        statsSiteProvider,
                        selectedDateProvider,
                        analyticsTracker,
                        useCaseMode
                )
    }
}
