package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType.PUBLICIZE
import org.wordpress.android.fluxc.store.stats.insights.PublicizeStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

private const val VIEW_ALL_ITEM_COUNT = 100

class PublicizeUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val publicizeStore: PublicizeStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val mapper: ServiceMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val popupMenuHandler: ItemPopupMenuHandler,
    private val useCaseMode: UseCaseMode
) : StatelessUseCase<PublicizeModel>(PUBLICIZE, mainDispatcher, backgroundDispatcher) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override suspend fun loadCachedData(): PublicizeModel? {
        return publicizeStore.getPublicizeData(
            statsSiteProvider.siteModel,
            LimitMode.Top(itemsToLoad)
        )
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<PublicizeModel> {
        val response = publicizeStore.fetchPublicizeData(
            statsSiteProvider.siteModel,
            LimitMode.Top(itemsToLoad),
            forced
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

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override fun buildUiModel(domainModel: PublicizeModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(buildTitle())
        }

        if (domainModel.services.isEmpty()) {
            items.add(Empty())
        } else {
            val header = Header(R.string.stats_publicize_service_label, R.string.stats_publicize_followers_label)
            items.add(header)
            items.addAll(domainModel.services.let {
                mapper.map(it, header)
            })
            if (useCaseMode == BLOCK && domainModel.hasMore) {
                items.add(
                    Link(
                        text = R.string.stats_insights_view_more,
                        navigateAction = ListItemInteraction.create(this::onLinkClick)
                    )
                )
            }
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_view_publicize, menuAction = this::onMenuClick)

    private fun onLinkClick() {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_PUBLICIZE_VIEW_MORE_TAPPED)
        return navigateTo(ViewPublicizeStats)
    }

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }

    class PublicizeUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val publicizeStore: PublicizeStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val mapper: ServiceMapper,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val popupMenuHandler: ItemPopupMenuHandler
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
            PublicizeUseCase(
                mainDispatcher,
                backgroundDispatcher,
                publicizeStore,
                statsSiteProvider,
                mapper,
                analyticsTracker,
                popupMenuHandler,
                useCaseMode
            )
    }
}
