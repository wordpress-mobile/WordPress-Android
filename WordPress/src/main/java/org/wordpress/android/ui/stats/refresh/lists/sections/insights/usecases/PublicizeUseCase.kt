package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.PUBLICIZE
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 5

class PublicizeUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val mapper: ServiceMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatelessUseCase<PublicizeModel>(PUBLICIZE, mainDispatcher) {
    override suspend fun loadCachedData(): PublicizeModel? {
        return insightsStore.getPublicizeData(
                statsSiteProvider.siteModel,
                PAGE_SIZE
        )
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<PublicizeModel> {
        val response = insightsStore.fetchPublicizeData(
                statsSiteProvider.siteModel,
                PAGE_SIZE, forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(
                    error.message ?: error.type.name
            )
            model != null && model.services.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_publicize))

    override fun buildUiModel(domainModel: PublicizeModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_publicize))
        if (domainModel.services.isEmpty()) {
            items.add(Empty())
        } else {
            items.add(Header(string.stats_publicize_service_label, string.stats_publicize_followers_label))
            items.addAll(domainModel.services.let { mapper.map(it) })
            if (domainModel.hasMore) {
                items.add(
                        Link(
                                text = string.stats_insights_view_more,
                                navigateAction = NavigationAction.create(this::onLinkClick)
                        )
                )
            }
        }
        return items
    }

    private fun onLinkClick() {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_PUBLICIZE_VIEW_MORE_TAPPED)
        return navigateTo(ViewPublicizeStats(statsSiteProvider.siteModel))
    }
}
