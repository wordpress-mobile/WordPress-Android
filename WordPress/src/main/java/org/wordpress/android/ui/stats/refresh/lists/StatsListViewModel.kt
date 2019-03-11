package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import org.wordpress.android.ui.stats.refresh.utils.toStatsGranularity
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.mapNullable
import org.wordpress.android.util.throttle
import org.wordpress.android.viewmodel.ScopedViewModel

const val SCROLL_EVENT_DELAY = 2000L

abstract class StatsListViewModel(
    defaultDispatcher: CoroutineDispatcher,
    private val statsUseCase: BaseListUseCase,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val dateSelector: StatsDateSelector,
    private val statsSection: StatsSection
) : ScopedViewModel(defaultDispatcher) {
    private var trackJob: Job? = null
    private var isInitialized = false

    enum class StatsSection(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months),
        YEARS(R.string.stats_timeframe_years);
    }

    val selectedDate = dateSelector.selectedDate

    val navigationTarget: LiveData<NavigationTarget> = statsUseCase.navigationTarget

    val listSelected = statsUseCase.listSelected

    val uiModel: LiveData<UiModel> = statsUseCase.data.throttle(this, distinct = true)

    val dateSelectorData: LiveData<DateSelectorUiModel> = dateSelector.dateSelectorData.mapNullable {
        it ?: DateSelectorUiModel(false)
    }

    override fun onCleared() {
        statsUseCase.onCleared()
        super.onCleared()
    }

    fun onScrolledToBottom() {
        if (trackJob?.isCompleted != false) {
            trackJob = launch {
                analyticsTracker.track(AnalyticsTracker.Stat.STATS_SCROLLED_TO_BOTTOM)
                delay(SCROLL_EVENT_DELAY)
            }
        }
    }

    fun onNextDateSelected() {
        launch(Dispatchers.Default) {
            statsSection.toStatsGranularity()?.let { granularity ->
                dateSelector.onNextDateSelected(granularity)
            }
        }
    }

    fun onPreviousDateSelected() {
        launch(Dispatchers.Default) {
            statsSection.toStatsGranularity()?.let { granularity ->
                dateSelector.onPreviousDateSelected(granularity)
            }
        }
    }

    fun onRetryClick() {
        launch {
            statsUseCase.refreshData(true)
        }
    }

    fun onDateChanged(granularity: StatsGranularity?) {
        launch {
            statsUseCase.onDateChanged()
        }
    }

    fun onListSelected() {
        dateSelector.updateDateSelector(statsSection.toStatsGranularity())
    }

    fun start() {
        if (!isInitialized) {
            isInitialized = true
            launch {
                statsUseCase.loadData()
                dateSelector.updateDateSelector(statsSection.toStatsGranularity())
            }
        }
        dateSelector.updateDateSelector(statsSection.toStatsGranularity())
    }

    sealed class UiModel {
        data class Success(val data: List<StatsBlock>) : UiModel()
        class Error(val message: Int = R.string.stats_loading_error) : UiModel()
    }
}
