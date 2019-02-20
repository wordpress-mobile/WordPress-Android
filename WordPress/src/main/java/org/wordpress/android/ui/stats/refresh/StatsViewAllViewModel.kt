package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Named

abstract class StatsViewAllViewModel(
    mainDispatcher: CoroutineDispatcher,
    protected val useCase: BaseListUseCase,
    private val dateFormatter: StatsDateFormatter,
    private val selectedDateProvider: SelectedDateProvider
) : StatsListViewModel(mainDispatcher, useCase) {
    companion object {
        fun get(type: StatsViewType, granularity: StatsGranularity?): Class<out StatsViewAllViewModel> {
            return when (granularity) {
                DAYS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> DailyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                WEEKS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> WeeklyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                MONTHS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> MonthlyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                YEARS -> {
                    when (type) {
                        StatsViewType.TOP_POSTS_AND_PAGES -> YearlyViewAllPostsAndPagesViewModel::class.java
                        StatsViewType.REFERRERS -> TODO()
                        StatsViewType.CLICKS -> TODO()
                        StatsViewType.AUTHORS -> TODO()
                        StatsViewType.GEOVIEWS -> TODO()
                        StatsViewType.SEARCH_TERMS -> TODO()
                        StatsViewType.VIDEO_PLAYS -> TODO()
                        else -> throw InvalidParameterException("Invalid time-based stats type: ${type.name}")
                    }
                }
                else -> {
                    when (type) {
                        StatsViewType.FOLLOWERS -> StatsViewAllFollowersViewModel::class.java
                        StatsViewType.COMMENTS -> StatsViewAllCommentsViewModel::class.java
                        StatsViewType.TAGS_AND_CATEGORIES -> StatsViewAllTagsAndCategoriesViewModel::class.java
                        StatsViewType.INSIGHTS_ALL_TIME -> TODO()
                        StatsViewType.INSIGHTS_LATEST_POST_SUMMARY -> TODO()
                        StatsViewType.INSIGHTS_MOST_POPULAR -> TODO()
                        StatsViewType.INSIGHTS_TODAY -> TODO()
                        StatsViewType.PUBLICIZE -> TODO()
                        else -> throw InvalidParameterException("Invalid insights stats type: ${type.name}")
                    }
                }
            }
        }
    }

    val selectedDateChanged = selectedDateProvider.selectedDateChanged

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val mutableShowDateSelector = MutableLiveData<DateSelectorUiModel>()
    val showDateSelector: LiveData<DateSelectorUiModel> = mutableShowDateSelector

    private lateinit var site: SiteModel
    private var statsGranularity: StatsGranularity? = null

    fun start(site: SiteModel, granularity: StatsGranularity?) {
        this.site = site
        this.statsGranularity = granularity

        loadData {
            updateDateSelector()
            useCase.loadData(site)
        }
    }

    fun onPullToRefresh() {
        loadData {
            useCase.refreshData(site, true)
        }
    }

    private fun CoroutineScope.loadData(executeLoading: suspend () -> Unit) = launch {
        _isRefreshing.value = true

        executeLoading()

        _isRefreshing.value = false
    }

    private fun updateDateSelector() {
        val shouldShowDateSelection = statsGranularity != null
        if (shouldShowDateSelection) {
            statsGranularity?.let { granularity ->
                val updatedDate = getDateLabelForSection(granularity)
                val currentState = showDateSelector.value
                if (!shouldShowDateSelection && currentState?.isVisible != false) {
                    emitValue(currentState, DateSelectorUiModel(false))
                } else {
                    val updatedState = DateSelectorUiModel(
                            shouldShowDateSelection,
                            updatedDate,
                            enableSelectPrevious = selectedDateProvider.hasPreviousDate(granularity),
                            enableSelectNext = selectedDateProvider.hasNextData(granularity)
                    )
                    emitValue(currentState, updatedState)
                }
            }
        } else {
            emitValue(showDateSelector.value, DateSelectorUiModel(false))
        }
    }

    fun onSelectedDateChange() {
        loadData {
            updateDateSelector()
            useCase.refreshData(site)
        }
    }

    fun onNextDateSelected() {
        launch(Dispatchers.Default) {
            statsGranularity?.let { granularity ->
                selectedDateProvider.selectNextDate(granularity)
            }
        }
    }

    fun onPreviousDateSelected() {
        launch(Dispatchers.Default) {
            statsGranularity?.let { granularity ->
                selectedDateProvider.selectPreviousDate(granularity)
            }
        }
    }

    private fun emitValue(
        currentState: DateSelectorUiModel?,
        updatedState: DateSelectorUiModel
    ) {
        if (currentState == null ||
                currentState.isVisible != updatedState.isVisible ||
                currentState.date != updatedState.date ||
                currentState.enableSelectNext != updatedState.enableSelectNext ||
                currentState.enableSelectPrevious != updatedState.enableSelectPrevious) {
            mutableShowDateSelector.value = updatedState
        }
    }

    private fun getDateLabelForSection(granularity: StatsGranularity): String? {
        return dateFormatter.printGranularDate(
                selectedDateProvider.getSelectedDate(granularity) ?: selectedDateProvider.getCurrentDate(),
                granularity
        )
    }
}

class StatsViewAllCommentsViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_COMMENTS_USE_CASE) useCase: BaseListUseCase,
    dateFormatter: StatsDateFormatter,
    selectedDateProvider: SelectedDateProvider
) : StatsViewAllViewModel(mainDispatcher, useCase, dateFormatter, selectedDateProvider)

class StatsViewAllFollowersViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_FOLLOWERS_USE_CASE) useCase: BaseListUseCase,
    dateFormatter: StatsDateFormatter,
    selectedDateProvider: SelectedDateProvider
) : StatsViewAllViewModel(mainDispatcher, useCase, dateFormatter, selectedDateProvider)

class StatsViewAllTagsAndCategoriesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_TAGS_AND_CATEGORIES_USE_CASE) useCase: BaseListUseCase,
    dateFormatter: StatsDateFormatter,
    selectedDateProvider: SelectedDateProvider
) : StatsViewAllViewModel(mainDispatcher, useCase, dateFormatter, selectedDateProvider)

// region ViewAllPostsAndPagesViewModels
class DailyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DAILY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateFormatter: StatsDateFormatter,
    selectedDateProvider: SelectedDateProvider
) : StatsViewAllViewModel(mainDispatcher, useCase, dateFormatter, selectedDateProvider)

class WeeklyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(WEEKLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateFormatter: StatsDateFormatter,
    selectedDateProvider: SelectedDateProvider
) : StatsViewAllViewModel(mainDispatcher, useCase, dateFormatter, selectedDateProvider)

class MonthlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(MONTHLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateFormatter: StatsDateFormatter,
    selectedDateProvider: SelectedDateProvider
) : StatsViewAllViewModel(mainDispatcher, useCase, dateFormatter, selectedDateProvider)

class YearlyViewAllPostsAndPagesViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(YEARLY_VIEW_ALL_POSTS_AND_PAGES_USE_CASE) useCase: BaseListUseCase,
    dateFormatter: StatsDateFormatter,
    selectedDateProvider: SelectedDateProvider
) : StatsViewAllViewModel(mainDispatcher, useCase, dateFormatter, selectedDateProvider)
// endregion
