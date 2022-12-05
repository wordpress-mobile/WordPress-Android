package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_LIKES
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalStatsMapper
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class TotalLikesDetailUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    statsGranularity: StatsGranularity,
    selectedDateProvider: SelectedDateProvider,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    statsSiteProvider: StatsSiteProvider,
    private val totalStatsMapper: TotalStatsMapper,
    private val statsWidgetUpdaters: StatsWidgetUpdaters
) : GranularStatefulUseCase<VisitsAndViewsModel, UiState>(
        TOTAL_LIKES,
        mainDispatcher,
        bgDispatcher,
        statsSiteProvider,
        selectedDateProvider,
        statsGranularity,
        UiState()
) {
    override fun buildLoadingItem() = listOf(TitleWithMore(string.stats_view_total_likes))

    override fun buildEmptyItem() = listOf(buildTitle(), Empty())

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): VisitsAndViewsModel? {
        statsWidgetUpdaters.updateViewsWidget(statsSiteProvider.siteModel.siteId)
        val cachedData = visitsAndViewsStore.getVisits(
                site,
                DAYS,
                LimitMode.Top(TOTAL_LIKES_ITEMS_TO_LOAD),
                selectedDate
        )
        if (cachedData != null) {
            selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
        }
        return cachedData
    }

    override suspend fun fetchRemoteData(
        selectedDate: Date,
        site: SiteModel,
        forced: Boolean
    ): State<VisitsAndViewsModel> {
        val response = visitsAndViewsStore.fetchVisits(
                statsSiteProvider.siteModel,
                DAYS,
                LimitMode.Top(TOTAL_LIKES_ITEMS_TO_LOAD),
                selectedDate,
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> {
                selectedDateProvider.onDateLoadingFailed(statsGranularity)
                State.Error(error.message ?: error.type.name)
            }
            model != null && model.dates.isNotEmpty() -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Data(model)
            }
            else -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Empty()
            }
        }
    }

    override fun buildUiModel(domainModel: VisitsAndViewsModel, uiState: UiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty()) {
            items.add(buildTitle())
            items.add(totalStatsMapper.buildTotalLikesValue(domainModel.dates))
            totalStatsMapper.buildTotalLikesInformation(domainModel.dates)?.let { items.add(it) }
        } else {
            selectedDateProvider.onDateLoadingFailed(statsGranularity)
            AppLog.e(T.STATS, "There is no data to be shown in the total likes block")
        }
        return items
    }

    private fun buildTitle() = TitleWithMore(string.stats_view_total_likes)

    companion object {
        private const val TOTAL_LIKES_ITEMS_TO_LOAD = 15
    }

    class TotalLikesGranularUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val selectedDateProvider: SelectedDateProvider,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val totalStatsMapper: TotalStatsMapper,
        private val statsWidgetUpdaters: StatsWidgetUpdaters
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                TotalLikesDetailUseCase(
                        mainDispatcher,
                        backgroundDispatcher,
                        granularity,
                        selectedDateProvider,
                        visitsAndViewsStore,
                        statsSiteProvider,
                        totalStatsMapper,
                        statsWidgetUpdaters
                )
    }
}
