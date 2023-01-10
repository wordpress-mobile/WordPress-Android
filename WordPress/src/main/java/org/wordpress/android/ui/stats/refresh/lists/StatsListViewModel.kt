package org.wordpress.android.ui.stats.refresh.lists

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.DAY_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.INSIGHTS_USE_CASE
import org.wordpress.android.ui.stats.refresh.MONTH_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewInsightsManagement
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.TOTAL_COMMENTS_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.TOTAL_FOLLOWERS_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.TOTAL_LIKES_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.VIEWS_AND_VISITORS_USE_CASE
import org.wordpress.android.ui.stats.refresh.WEEK_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.YEAR_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.NewsCardHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.mapNullable
import org.wordpress.android.util.merge
import org.wordpress.android.util.mergeNotNull
import org.wordpress.android.util.throttle
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val SCROLL_EVENT_DELAY = 2000L

abstract class StatsListViewModel(
    defaultDispatcher: CoroutineDispatcher,
    private val statsUseCase: BaseListUseCase,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    protected val dateSelector: StatsDateSelector,
    popupMenuHandler: ItemPopupMenuHandler? = null,
    private val newsCardHandler: NewsCardHandler? = null,
    actionCardHandler: ActionCardHandler? = null
) : ScopedViewModel(defaultDispatcher) {
    private var trackJob: Job? = null
    private var isInitialized = false

    enum class StatsSection(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months),
        YEARS(R.string.stats_timeframe_years),
        DETAIL(R.string.stats),
        INSIGHT_DETAIL(R.string.stats_insights_views_and_visitors),
        TOTAL_LIKES_DETAIL(R.string.stats_view_total_likes),
        TOTAL_COMMENTS_DETAIL(R.string.stats_view_total_comments),
        TOTAL_FOLLOWERS_DETAIL(R.string.stats_view_total_followers),
        ANNUAL_STATS(R.string.stats_insights_annual_site_stats);
    }

    val selectedDate = dateSelector.selectedDate

    private val mutableNavigationTarget = MutableLiveData<Event<NavigationTarget>>()
    val navigationTarget: LiveData<Event<NavigationTarget>> = mergeNotNull(
        statsUseCase.navigationTarget, mutableNavigationTarget
    )

    val listSelected = statsUseCase.listSelected

    val uiModel: LiveData<UiModel?> by lazy {
        statsUseCase.data.throttle(viewModelScope, distinct = true)
    }

    val dateSelectorData: LiveData<DateSelectorUiModel> = dateSelector.dateSelectorData.mapNullable {
        it ?: DateSelectorUiModel(false)
    }

    val typesChanged = merge(
        popupMenuHandler?.typeMoved,
        newsCardHandler?.cardDismissed,
        actionCardHandler?.actionCard
    )

    val scrollTo = newsCardHandler?.scrollTo

    val scrollToNewCard = statsUseCase.scrollTo

    override fun onCleared() {
        statsUseCase.onCleared()
        super.onCleared()
    }

    fun onScrolledToBottom() {
        if (trackJob?.isCompleted != false) {
            trackJob = launch {
                analyticsTracker.track(Stat.STATS_SCROLLED_TO_BOTTOM)
                delay(SCROLL_EVENT_DELAY)
            }
        }
    }

    fun onNextDateSelected() {
        launch(Dispatchers.Default) {
            dateSelector.onNextDateSelected()
        }
    }

    fun onPreviousDateSelected() {
        launch(Dispatchers.Default) {
            dateSelector.onPreviousDateSelected()
        }
    }

    fun onRetryClick() {
        launch {
            statsUseCase.refreshData(true)
        }
    }

    fun onDateChanged(selectedSection: StatsSection) {
        launch {
            statsUseCase.onDateChanged(selectedSection)
        }
    }

    fun onListSelected() {
        dateSelector.updateDateSelector()
    }

    fun onEmptyInsightsButtonClicked() {
        mutableNavigationTarget.value = Event(ViewInsightsManagement)
    }

    fun onAddNewStatsButtonClicked() {
        newsCardHandler?.dismiss()
        analyticsTracker.track(Stat.STATS_INSIGHTS_MANAGEMENT_ACCESSED, mapOf("source" to "button"))
        mutableNavigationTarget.value = Event(ViewInsightsManagement)
    }

    fun start() {
        if (!isInitialized) {
            isInitialized = true
            launch {
                statsUseCase.loadData()
                dateSelector.updateDateSelector()
            }
        }
        dateSelector.updateDateSelector()
    }

    sealed class UiModel {
        data class Success(val data: List<StatsBlock>) : UiModel()
        data class Error(val message: Int = R.string.stats_loading_error) : UiModel()
        data class Empty(
            val title: Int,
            val subtitle: Int? = null,
            val image: Int? = null,
            val showButton: Boolean = false
        ) : UiModel()
    }

    fun onTypesChanged() {
        launch {
            statsUseCase.refreshTypes()
        }
    }
}

class InsightsListViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(INSIGHTS_USE_CASE) private val insightsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory,
    popupMenuHandler: ItemPopupMenuHandler,
    newsCardHandler: NewsCardHandler,
    actionCardHandler: ActionCardHandler
) : StatsListViewModel(
    mainDispatcher,
    insightsUseCase,
    analyticsTracker,
    dateSelectorFactory.build(INSIGHTS),
    popupMenuHandler,
    newsCardHandler,
    actionCardHandler
)

class YearsListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(YEAR_STATS_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(YEARS))

class MonthsListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(MONTH_STATS_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(MONTHS))

class WeeksListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(WEEK_STATS_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(WEEKS))

class DaysListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DAY_STATS_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(DAYS))

// Using Weeks granularity on new insight details screens
class InsightsDetailListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEWS_AND_VISITORS_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(WEEKS))

class TotalLikesDetailListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(TOTAL_LIKES_DETAIL_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(WEEKS))

class TotalCommentsDetailListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(TOTAL_COMMENTS_DETAIL_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(WEEKS))

class TotalFollowersDetailListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(TOTAL_FOLLOWERS_DETAIL_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker, dateSelectorFactory.build(WEEKS))
