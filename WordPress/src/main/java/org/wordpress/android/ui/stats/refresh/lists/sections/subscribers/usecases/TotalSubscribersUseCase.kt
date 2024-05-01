package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.stats.insights.SummaryStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject
import javax.inject.Named

class TotalSubscribersUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val summaryStore: SummaryStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsUtils: StatsUtils
) : StatelessUseCase<Int>(StatsStore.SubscriberType.TOTAL_SUBSCRIBERS, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_total_subscribers))

    override fun buildEmptyItem() = buildUiModel(0)

    override suspend fun loadCachedData() = summaryStore.getSummary(statsSiteProvider.siteModel)?.followers

    override suspend fun fetchRemoteData(forced: Boolean): State<Int> {
        val response = summaryStore.fetchSummary(statsSiteProvider.siteModel, forced)
        val model = response.model?.followers
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: Int): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())
        items.add(ValueWithChartItem(
            value = statsUtils.toFormattedString(domainModel, MILLION),
            extraBottomMargin = true
        ))
        return items
    }

    private fun buildTitle() = Title(R.string.stats_view_total_subscribers)

    class TotalSubscribersUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val summaryStore: SummaryStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val statsUtils: StatsUtils
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) = TotalSubscribersUseCase(
            mainDispatcher,
            backgroundDispatcher,
            summaryStore,
            statsSiteProvider,
            statsUtils
        )
    }
}
