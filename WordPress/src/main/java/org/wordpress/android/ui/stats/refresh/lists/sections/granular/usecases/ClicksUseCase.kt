package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.ClicksModel
import org.wordpress.android.fluxc.model.stats.time.ClicksModel.Group
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.CLICKS
import org.wordpress.android.fluxc.store.stats.time.ClicksStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.VIEW_ALL_ITEM_COUNT
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
import org.wordpress.android.ui.utils.ListItemInteraction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ClicksUseCase.SelectedClicksGroup
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
class ClicksUseCase constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val store: ClicksStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val useCaseMode: UseCaseMode
) : GranularStatefulUseCase<ClicksModel, SelectedClicksGroup>(
        CLICKS,
        mainDispatcher,
        backgroundDispatcher,
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

    override fun buildUiModel(domainModel: ClicksModel, uiState: SelectedClicksGroup): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_clicks))
        }

        if (domainModel.groups.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            addDomainModelGroup(items, domainModel, uiState)
        }
        return items
    }

    private fun addDomainModelGroup(
        items: MutableList<BlockListItem>,
        domainModel: ClicksModel,
        uiState: SelectedClicksGroup
    ) {
        val header = Header(string.stats_clicks_link_label, string.stats_clicks_label)
        items.add(header)
        domainModel.groups.forEachIndexed { index, group ->
            val groupName = group.name
            val contentDescription = contentDescriptionHelper.buildContentDescription(
                    header,
                    groupName ?: "",
                    group.views ?: 0
            )
            val headerItem = ListItemWithIcon(
                    text = groupName,
                    value = statsUtils.toFormattedString(group.views),
                    showDivider = index < domainModel.groups.size - 1,
                    navigationAction = group.url?.let { create(it, this::onItemClick) },
                    contentDescription = contentDescription
            )
            if (group.clicks.isEmpty()) {
                items.add(headerItem)
            } else {
                addGroup(group, uiState, items, headerItem, header)
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

    private fun addGroup(
        group: Group,
        uiState: SelectedClicksGroup,
        items: MutableList<BlockListItem>,
        headerItem: ListItemWithIcon,
        header: Header
    ) {
        val isExpanded = group == uiState.group
        items.add(ExpandableItem(headerItem, isExpanded) { changedExpandedState ->
            onUiState(SelectedClicksGroup(if (changedExpandedState) group else null))
        })
        if (isExpanded) {
            items.addAll(group.clicks.map { click ->
                ListItemWithIcon(
                        text = click.name,
                        textStyle = LIGHT,
                        value = statsUtils.toFormattedString(click.views),
                        showDivider = false,
                        navigationAction = click.url?.let { create(it, this::onItemClick) },
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                                header,
                                click.name,
                                click.views
                        )
                )
            })
            items.add(Divider)
        }
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
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val store: ClicksStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val contentDescriptionHelper: ContentDescriptionHelper,
        private val statsUtils: StatsUtils,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                ClicksUseCase(
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
