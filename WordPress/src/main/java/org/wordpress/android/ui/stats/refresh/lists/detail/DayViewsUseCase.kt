package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Day
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes
import org.wordpress.android.fluxc.store.stats.PostDetailStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.detail.DayViewsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class DayViewsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val dayViewsMapper: DayViewsMapper,
    private val statsDateFormatter: StatsDateFormatter,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsPostProvider: StatsPostProvider,
    private val postDetailStore: PostDetailStore
) : BaseStatsUseCase<PostDetailStatsModel, UiState>(
        PostDetailTypes.POST_OVERVIEW,
        mainDispatcher,
        UiState()
) {
    init {
        uiState.addSource(selectedDateProvider.granularSelectedDateChanged(DETAIL)) {
            onUiState()
        }
    }

    override suspend fun loadCachedData(): PostDetailStatsModel? {
        return statsPostProvider.postId?.let { postId ->
            postDetailStore.getPostingActivity(
                    statsSiteProvider.siteModel,
                    postId
            )
        }
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<PostDetailStatsModel> {
        val response = statsPostProvider.postId?.let { postId ->
            postDetailStore.fetchPostDetail(statsSiteProvider.siteModel, postId, forced)
        }
        val model = response?.model
        val error = response?.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.dayViews.isNotEmpty() -> State.Data(
                    model
            )
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: PostDetailStatsModel, uiState: UiState): List<BlockListItem> {
        val periodFromProvider = selectedDateProvider.getSelectedDate(DETAIL)
        if (periodFromProvider == null) {
            val dates = domainModel.dayViews.map { statsDateFormatter.parseStatsDate(DAYS, it.period) }
            selectedDateProvider.selectDate(dates.lastIndex, dates, DETAIL)
        }
        val selectedPeriod = periodFromProvider?.let { statsDateFormatter.printStatsDate(periodFromProvider) }
                ?: domainModel.dayViews.last().period
        var previousDay: Day? = null
        var selectedDay: Day? = null
        for (index in 0 until domainModel.dayViews.size) {
            val day = domainModel.dayViews[index]
            selectedDay = day
            if (selectedPeriod == day.period) {
                break
            }
            if (index < domainModel.dayViews.size - 1) {
                previousDay = day
            }
        }
        val items = mutableListOf<BlockListItem>()
        items.add(dayViewsMapper.buildTitle(selectedDay ?: domainModel.dayViews.last(), previousDay))
        items.addAll(
                dayViewsMapper.buildChart(
                        domainModel.dayViews,
                        selectedPeriod,
                        this::onBarSelected,
                        this::onBarChartDrawn
                )
        )
        return items
    }

    override fun buildLoadingItem(): List<BlockListItem> {
        return listOf(ValueItem(value = 0.toFormattedString(), unit = string.stats_views, isFirst = true))
    }

    private fun onBarSelected(period: String?) {
        if (period != null && period != "empty") {
            val selectedDate = statsDateFormatter.parseStatsDate(DAYS, period)
            selectedDateProvider.selectDate(
                    selectedDate,
                    DETAIL
            )
        }
    }

    private fun onBarChartDrawn(visibleBarCount: Int) {
        updateUiState { it.copy(visibleBarCount = visibleBarCount) }
    }

    data class UiState(val visibleBarCount: Int? = null)
}
