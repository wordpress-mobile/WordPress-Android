package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.REFERRERS
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase.SelectedGroup
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class ReferrersUseCase
constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val referrersStore: ReferrersStore,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : GranularStatefulUseCase<ReferrersModel, SelectedGroup>(
        REFERRERS,
        mainDispatcher,
        selectedDateProvider,
        statsGranularity,
        SelectedGroup()
) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_referrers))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): ReferrersModel? {
        return referrersStore.getReferrers(
                site,
                statsGranularity,
                selectedDate,
                PAGE_SIZE
        )
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<ReferrersModel> {
        val response = referrersStore.fetchReferrers(
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
            model != null && model.groups.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildStatefulUiModel(domainModel: ReferrersModel, uiState: SelectedGroup): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_referrers))

        if (domainModel.groups.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            items.add(Header(R.string.stats_referrer_label, R.string.stats_referrer_views_label))
            domainModel.groups.forEachIndexed { index, group ->
                if (group.referrers.isEmpty()) {
                    val headerItem = ListItemWithIcon(
                            iconUrl = group.icon,
                            text = group.name,
                            value = group.total?.toFormattedString(),
                            showDivider = index < domainModel.groups.size - 1,
                            navigationAction = group.url?.let { create(it, this::onItemClick) }
                    )
                    items.add(headerItem)
                } else {
                    val headerItem = ListItemWithIcon(
                            iconUrl = group.icon,
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
                            ListItemWithIcon(
                                    iconUrl = referrer.icon,
                                    text = referrer.name,
                                    value = referrer.views.toFormattedString(),
                                    showDivider = false,
                                    navigationAction = referrer.url?.let { create(it, this::onItemClick) }
                            )
                        })
                        items.add(Divider)
                    }
                }
                null
            }

            if (domainModel.hasMore) {
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
        private val selectedDateProvider: SelectedDateProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                ReferrersUseCase(
                        granularity,
                        mainDispatcher,
                        referrersStore,
                        selectedDateProvider,
                        analyticsTracker
                )
    }
}
