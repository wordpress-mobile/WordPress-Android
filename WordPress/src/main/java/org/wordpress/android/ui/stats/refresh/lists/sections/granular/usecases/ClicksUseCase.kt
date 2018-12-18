package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.ClicksModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.CLICKS
import org.wordpress.android.fluxc.store.stats.time.ClicksStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ClicksUseCase.SelectedClicksGroup
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class ClicksUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: ClicksStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter
) : StatefulUseCase<ClicksModel, SelectedClicksGroup>(CLICKS, mainDispatcher, SelectedClicksGroup()) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_clicks))

    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = store.getClicks(
                site,
                statsGranularity,
                PAGE_SIZE,
                selectedDateProvider.getSelectedDate(statsGranularity)
        )
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = store.fetchClicks(
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

    override fun buildStatefulUiModel(domainModel: ClicksModel, uiState: SelectedClicksGroup): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_clicks))

        if (domainModel.groups.isEmpty()) {
            items.add(Empty)
        } else {
            items.add(Header(R.string.stats_clicks_link_label, R.string.stats_clicks_label))
            domainModel.groups.forEachIndexed { index, group ->
                val headerItem = ListItemWithIcon(
                        iconUrl = group.icon,
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
                                    iconUrl = click.icon,
                                    text = click.name,
                                    value = click.views.toFormattedString(),
                                    showDivider = false,
                                    navigationAction = click.url?.let { NavigationAction.create(it, this::onItemClick) }
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

    private fun onItemClick(url: String) {
        navigateTo(ViewUrl(url))
    }

    data class SelectedClicksGroup(val group: ClicksModel.Group? = null)

    class ClicksUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val store: ClicksStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                ClicksUseCase(
                        granularity,
                        mainDispatcher,
                        store,
                        selectedDateProvider,
                        statsDateFormatter
                )
    }
}
