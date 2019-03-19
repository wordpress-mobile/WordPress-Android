package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.REFERRERS
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewReferrers
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
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.EMPTY_SPACE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase.SelectedGroup
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_ITEM_COUNT = 1000

class ReferrersUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val referrersStore: ReferrersStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : GranularStatefulUseCase<ReferrersModel, SelectedGroup>(
        REFERRERS,
        mainDispatcher,
        statsSiteProvider,
        selectedDateProvider,
        statsGranularity,
        SelectedGroup()
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_referrers))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): ReferrersModel? {
        return referrersStore.getReferrers(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<ReferrersModel> {
        val response = referrersStore.fetchReferrers(
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

    override fun buildStatefulUiModel(domainModel: ReferrersModel, uiState: SelectedGroup): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_referrers))
        }

        if (domainModel.groups.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            items.add(Header(R.string.stats_referrer_label, R.string.stats_referrer_views_label))
            domainModel.groups.forEachIndexed { index, group ->
                val icon = buildIcon(group.icon)
                if (group.referrers.isEmpty()) {
                    val headerItem = ListItemWithIcon(
                            icon = icon,
                            iconUrl = if (icon == null) group.icon else null,
                            text = group.name,
                            value = group.total?.toFormattedString(),
                            showDivider = index < domainModel.groups.size - 1,
                            navigationAction = group.url?.let { create(it, this::onItemClick) }
                    )
                    items.add(headerItem)
                } else {
                    val headerItem = ListItemWithIcon(
                            icon = icon,
                            iconUrl = if (icon == null) group.icon else null,
                            text = group.name,
                            value = group.total?.toFormattedString(),
                            showDivider = index < domainModel.groups.size - 1
                    )
                    val isExpanded = group.groupId == uiState.groupId
                    items.add(ExpandableItem(headerItem, isExpanded) { changedExpandedState ->
                        onUiState(SelectedGroup(if (changedExpandedState) group.groupId else null))
                    })
                    if (isExpanded) {
                        items.addAll(group.referrers.map { referrer ->
                            val referrerIcon = buildIcon(referrer.icon)
                            val iconStyle = if (group.icon != null && referrer.icon == null && referrerIcon == null) {
                                EMPTY_SPACE
                            } else {
                                NORMAL
                            }
                            ListItemWithIcon(
                                    icon = referrerIcon,
                                    iconUrl = if (referrerIcon == null) referrer.icon else null,
                                    iconStyle = iconStyle,
                                    textStyle = LIGHT,
                                    text = referrer.name,
                                    value = referrer.views.toFormattedString(),
                                    showDivider = false,
                                    navigationAction = referrer.url?.let { create(it, this::onItemClick) }
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
                                navigateAction = create(statsGranularity, this::onViewMoreClicked)
                        )
                )
            }
        }
        return items
    }

    private fun buildIcon(iconUrl: String?): Int? {
        return when (iconUrl) {
            null -> R.drawable.ic_globe_white_24dp
            "https://wordpress.com/i/stats/search-engine.png" -> R.drawable.ic_search_white_24dp
            else -> null
        }
    }

    private fun onViewMoreClicked(statsGranularity: StatsGranularity) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_REFERRERS_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewReferrers(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
                )
        )
    }

    private fun onItemClick(url: String) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_REFERRERS_ITEM_TAPPED, statsGranularity)
        navigateTo(ViewUrl(url))
    }

    data class SelectedGroup(val groupId: String? = null)

    class ReferrersUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val referrersStore: ReferrersStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                ReferrersUseCase(
                        granularity,
                        mainDispatcher,
                        referrersStore,
                        statsSiteProvider,
                        selectedDateProvider,
                        analyticsTracker,
                        useCaseMode
                )
    }
}
