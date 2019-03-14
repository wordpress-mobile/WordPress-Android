package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.ClicksModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.CLICKS
import org.wordpress.android.fluxc.store.stats.time.ClicksStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ClicksUseCase.SelectedClicksGroup
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_ITEM_COUNT = 1000

class ClicksUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: ClicksStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : GranularStatefulUseCase<ClicksModel, SelectedClicksGroup>(
        CLICKS,
        mainDispatcher,
        statsSiteProvider,
        selectedDateProvider,
        statsGranularity,
        SelectedClicksGroup()
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_clicks))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): ClicksModel? {
        return store.getClicks(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<ClicksModel> {
        val response = store.fetchClicks(
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
            model != null && model.groups.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildStatefulUiModel(domainModel: ClicksModel, uiState: SelectedClicksGroup): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_clicks))
        }

        if (domainModel.groups.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            items.add(Header(R.string.stats_clicks_link_label, R.string.stats_clicks_label))
            domainModel.groups.forEachIndexed { index, group ->
                val headerItem = ListItemWithIcon(
                        text = group.name,
                        value = group.views?.toFormattedString(),
                        showDivider = index < domainModel.groups.size - 1,
                        navigationAction = group.url?.let { NavigationAction.create(it, this::onItemClick) }
                )
                if (group.clicks.isEmpty()) {
                    items.add(headerItem)
                } else {
                    val isExpanded = group == uiState.group
                    items.add(ExpandableItem(headerItem, isExpanded) { changedExpandedState ->
                        onUiState(SelectedClicksGroup(if (changedExpandedState) group else null))
                    })
                    if (isExpanded) {
                        items.addAll(group.clicks.map { click ->
                            ListItemWithIcon(
                                    text = click.name,
                                    textStyle = LIGHT,
                                    value = click.views.toFormattedString(),
                                    showDivider = false,
                                    navigationAction = click.url?.let { NavigationAction.create(it, this::onItemClick) }
                            )
                        })
                        items.add(Divider)
                    }
                }
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
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_CLICKS_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewClicks(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
                )
        )
    }

    private fun onItemClick(url: String) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_CLICKS_ITEM_TAPPED, statsGranularity)
        navigateTo(ViewUrl(url))
    }

    data class SelectedClicksGroup(val group: ClicksModel.Group? = null)

    class ClicksUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val store: ClicksStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                ClicksUseCase(
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
