package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType
import org.wordpress.android.fluxc.store.stats.PostDetailStore
import org.wordpress.android.modules.BG_THREAD
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
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class PostDayViewsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val postDayViewsMapper: PostDayViewsMapper,
    private val statsDateFormatter: StatsDateFormatter,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsPostProvider: StatsPostProvider,
    private val postDetailStore: PostDetailStore,
    private val resourceProvider: ResourceProvider
) : BaseStatsUseCase<PostDetailStatsModel, UiState>(
        PostDetailType.POST_OVERVIEW,
        mainDispatcher,
        backgroundDispatcher,
        UiState(),
        uiUpdateParams = listOf(UseCaseParam.SelectedDateParam(DETAIL))
) {
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
                selectedDateProvider.onDateLoadingFailed(DETAIL)
                State.Error(error.message ?: error.type.name)
            }
            model != null && model.hasData() -> {
                selectedDateProvider.onDateLoadingSucceeded(DETAIL)
                State.Data(model)
            }
            else -> {
                selectedDateProvider.onDateLoadingSucceeded(DETAIL)
                State.Empty()
            }
        }
    }

    override fun buildUiModel(domainModel: PostDetailStatsModel, uiState: UiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        val visibleBarCount = uiState.visibleBarCount ?: domainModel.dayViews.size

        if (domainModel.hasData() && visibleBarCount > 0) {
            val periodFromProvider = selectedDateProvider.getSelectedDate(DETAIL)
            val availablePeriods = domainModel.dayViews.takeLast(visibleBarCount)
            val availableDates = availablePeriods.map { statsDateFormatter.parseStatsDate(DAYS, it.period) }

            val selectedPeriod = periodFromProvider ?: availableDates.last()
            val index = availableDates.indexOf(selectedPeriod)

            selectedDateProvider.selectDate(selectedPeriod, availableDates, DETAIL)

            val shiftedIndex = index + domainModel.dayViews.size - visibleBarCount
            val selectedItem = domainModel.dayViews.getOrNull(shiftedIndex) ?: domainModel.dayViews.last()
            val previousItem = domainModel.dayViews.getOrNull(domainModel.dayViews.indexOf(selectedItem) - 1)

            items.add(
                    postDayViewsMapper.buildTitle(
                            selectedItem,
                            previousItem,
                            isLast = selectedItem == domainModel.dayViews.last()
                    )
            )
            items.addAll(
                    postDayViewsMapper.buildChart(
                            domainModel.dayViews,
                            selectedItem.period,
                            this::onBarSelected,
                            this::onBarChartDrawn
                    )
            )
        } else {
            selectedDateProvider.onDateLoadingFailed(DETAIL)
            AppLog.e(T.STATS, "There is no data to be shown in the post day view block")
        }
        return items
    }

    override fun buildLoadingItem(): List<BlockListItem> {
        return listOf(
                ValueItem(
                        value = "0",
                        unit = R.string.stats_views,
                        isFirst = true,
                        contentDescription = resourceProvider.getString(R.string.stats_loading_card)
                )
        )
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

    private fun PostDetailStatsModel?.hasData(): Boolean {
        return this != null && this.dayViews.isNotEmpty() && this.dayViews.any { it.count > 0 }
    }

    data class UiState(val visibleBarCount: Int? = null)
}
