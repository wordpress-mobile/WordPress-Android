package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.CountryViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.COUNTRIES
import org.wordpress.android.fluxc.store.stats.time.CountryViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCountries
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.VIEW_ALL_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapLegend
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
class CountryViewsUseCase constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val store: CountryViewsStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val useCaseMode: UseCaseMode
) : GranularStatelessUseCase<CountryViewsModel>(
        COUNTRIES,
        mainDispatcher,
        backgroundDispatcher,
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
            var minViews: Int? = null
            var maxViews: Int? = null
            for (country in domainModel.countries) {
                if (country.views < minViews ?: Int.MAX_VALUE) {
                    minViews = country.views
                }
                if (country.views > maxViews ?: 0) {
                    maxViews = country.views
                }
                stringBuilder.append("['").append(country.countryCode).append("',").append(country.views).append("],")
            }
            val startLabel = if (minViews == maxViews) {
                0
            } else {
                minViews ?: 0
            }.let { statsUtils.toFormattedString(it) }
            val endLabel = statsUtils.toFormattedString(maxViews, defaultValue = "0")
            items.add(MapItem(stringBuilder.toString(), R.string.stats_country_views_label))
            items.add(MapLegend(startLabel, endLabel))
            val header = Header(R.string.stats_country_label, R.string.stats_country_views_label)
            items.add(header)
            domainModel.countries.forEachIndexed { index, group ->
                items.add(
                        ListItemWithIcon(
                                iconUrl = group.flagIconUrl,
                                text = group.fullName,
                                value = statsUtils.toFormattedString(group.views),
                                showDivider = index < domainModel.countries.size - 1,
                                contentDescription = contentDescriptionHelper.buildContentDescription(
                                        header,
                                        group.fullName,
                                        group.views
                                )
                        )
                )
            }

            if (useCaseMode != VIEW_ALL && domainModel.hasMore) {
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
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val store: CountryViewsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsUtils: StatsUtils,
        private val contentDescriptionHelper: ContentDescriptionHelper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                CountryViewsUseCase(
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
