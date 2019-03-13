package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes
import org.wordpress.android.fluxc.store.stats.PostDetailStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.detail.PostDayViewsUseCase.UiState
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

class PostDayViewsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val postDayViewsMapper: PostDayViewsMapper,
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
            postDetailStore.getPostDetail(
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
            error != null -> {
                selectedDateProvider.dateLoadingSucceeded(DETAIL)
                State.Error(error.message ?: error.type.name)
            }
            model != null && model.dayViews.isNotEmpty() -> {
                selectedDateProvider.dateLoadingSucceeded(DETAIL)
                State.Data(model)
            }
            else -> {
                selectedDateProvider.dateLoadingSucceeded(DETAIL)
                State.Empty()
            }
        }
    }

    override fun buildUiModel(domainModel: PostDetailStatsModel, uiState: UiState): List<BlockListItem> {
        val periodFromProvider = selectedDateProvider.getSelectedDate(DETAIL)
        val visibleBarCount = uiState.visibleBarCount ?: domainModel.dayViews.size
        val availablePeriods = domainModel.dayViews.takeLast(visibleBarCount)
        val availableDates = availablePeriods.map { statsDateFormatter.parseStatsDate(DAYS, it.period) }
        val selectedPeriod = periodFromProvider ?: availableDates.last()
        val index = availableDates.indexOf(selectedPeriod)

        selectedDateProvider.selectDate(index, availableDates, DETAIL)

        val shiftedIndex = index + domainModel.dayViews.size - visibleBarCount
        val selectedItem = domainModel.dayViews.getOrNull(shiftedIndex) ?: domainModel.dayViews.last()
        val previousItem = domainModel.dayViews.getOrNull(domainModel.dayViews.indexOf(selectedItem) - 1)

        val items = mutableListOf<BlockListItem>()
        items.add(postDayViewsMapper.buildTitle(selectedItem, previousItem))
        items.addAll(
                postDayViewsMapper.buildChart(
                        domainModel.dayViews,
                        selectedItem.period,
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
