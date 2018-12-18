package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.REFERRERS
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase.SelectedGroup
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class ReferrersUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val referrersStore: ReferrersStore,
    private val statsDateFormatter: StatsDateFormatter,
    private val selectedDateProvider: SelectedDateProvider
) : StatefulUseCase<ReferrersModel, SelectedGroup>(REFERRERS, mainDispatcher, SelectedGroup()) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_referrers))

    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = referrersStore.getReferrers(
                site,
                statsGranularity,
                selectedDateProvider.getSelectedDate(statsGranularity),
                PAGE_SIZE
        )
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = referrersStore.fetchReferrers(
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

    override fun buildStatefulUiModel(domainModel: ReferrersModel, uiState: SelectedGroup): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_referrers))

        if (domainModel.groups.isEmpty()) {
            items.add(Empty)
        } else {
            items.add(Header(R.string.stats_referrer_label, R.string.stats_referrer_views_label))
            domainModel.groups.forEachIndexed { index, group ->
                val headerItem = ListItemWithIcon(
                        iconUrl = group.icon,
                        text = group.name,
                        value = group.total?.toFormattedString(),
                        showDivider = index < domainModel.groups.size - 1
                )
                if (group.referrers.isEmpty()) {
                    items.add(headerItem)
                } else {
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
                                    showDivider = false
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
                                navigateAction = create(statsGranularity, this::onViewMoreClicked)
                        )
                )
            }
        }
        return items
    }

    private fun onViewMoreClicked(statsGranularity: StatsGranularity) {
        navigateTo(ViewReferrers(statsGranularity, statsDateFormatter.todaysDateInStatsFormat()))
    }

    data class SelectedGroup(val groupId: String? = null)

    class ReferrersUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val referrersStore: ReferrersStore,
        private val statsDateFormatter: StatsDateFormatter,
        private val selectedDateProvider: SelectedDateProvider
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                ReferrersUseCase(
                        granularity,
                        mainDispatcher,
                        referrersStore,
                        statsDateFormatter,
                        selectedDateProvider
                )
    }
}
