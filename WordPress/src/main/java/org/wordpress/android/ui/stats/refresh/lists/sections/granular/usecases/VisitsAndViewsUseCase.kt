package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.OVERVIEW
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.VisitsAndViewsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 15

class VisitsAndViewsUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : StatefulUseCase<VisitsAndViewsModel, UiState>(OVERVIEW, mainDispatcher, UiState()) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = visitsAndViewsStore.getVisits(
                site,
                selectedDateProvider.getSelectedDate(statsGranularity),
                statsGranularity
        )
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = visitsAndViewsStore.fetchVisits(
                site,
                PAGE_SIZE,
                selectedDateProvider.getSelectedDate(statsGranularity),
                statsGranularity,
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

    override fun buildStatefulUiModel(domainModel: VisitsAndViewsModel, uiState: UiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(text = statsDateFormatter.parseDate(domainModel.period)))
        val selectedDate = uiState.selectedDate ?: domainModel.period
        val selectedItem = domainModel.dates.find { it.period == selectedDate } ?: domainModel.dates.lastOrNull()
        val chartItems = domainModel.dates.map {
            val value = when (uiState.selectedPosition) {
                0 -> it.views
                1 -> it.visitors
                2 -> it.likes
                3 -> it.comments
                else -> 0L
            }
            BarChartItem.Bar(
                    statsDateFormatter.parseGranularDate(it.period, statsGranularity),
                    it.period,
                    value.toInt()
            )
        }
        items.add(BarChartItem(chartItems, selectedItem = selectedDate, onBarSelected = this::onBarSelected))
        items.add(
                Columns(
                        listOf(
                                R.string.stats_views,
                                R.string.stats_visitors,
                                R.string.stats_likes,
                                R.string.stats_comments
                        ),
                        listOf(
                                selectedItem?.views?.toFormattedString() ?: "0",
                                selectedItem?.visitors?.toFormattedString() ?: "0",
                                selectedItem?.likes?.toFormattedString() ?: "0",
                                selectedItem?.comments?.toFormattedString() ?: "0"
                        ),
                        uiState.selectedPosition,
                        this::onColumnSelected
                )
        )
        return items
    }

    private fun onBarSelected(period: String?) {
        updateUiState { previousState -> previousState.copy(selectedDate = period) }
    }

    private fun onColumnSelected(position: Int) {
        updateUiState { previousState -> previousState.copy(selectedPosition = position) }
    }

    data class UiState(
        val selectedPosition: Int = 0,
        val selectedDate: String? = null
    )

    class VisitsAndViewsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val visitsAndViewsStore: VisitsAndViewsStore
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                VisitsAndViewsUseCase(
                        granularity,
                        visitsAndViewsStore,
                        selectedDateProvider,
                        statsDateFormatter,
                        mainDispatcher
                )
    }
}
