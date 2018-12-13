package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.CountryViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.COUNTRIES
import org.wordpress.android.fluxc.store.stats.time.CountryViewsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewClicks
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

class CountryViewsUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: CountryViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter
) : StatelessUseCase<CountryViewsModel>(COUNTRIES, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = store.getCountryViews(
                site,
                statsGranularity,
                PAGE_SIZE,
                selectedDateProvider.getSelectedDate(statsGranularity)
        )
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = store.fetchCountryViews(
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

    override fun buildUiModel(domainModel: CountryViewsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_countries))

        if (domainModel.countries.isEmpty()) {
            items.add(Empty)
        } else {
            items.add(Label(R.string.stats_country_label, R.string.stats_country_views_label))
            domainModel.countries.forEachIndexed { index, group ->
                items.add(
                        ListItemWithIcon(
                                iconUrl = group.flatFlagIconUrl,
                                text = group.fullName,
                                value = group.views.toFormattedString(),
                                showDivider = index < domainModel.countries.size - 1
                        )
                )
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
        navigateTo(ViewClicks(statsGranularity, statsDateFormatter.todaysDateInStatsFormat()))
    }

    class CountryViewsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val store: CountryViewsStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                CountryViewsUseCase(
                        granularity,
                        mainDispatcher,
                        store,
                        selectedDateProvider,
                        statsDateFormatter
                )
    }
}
