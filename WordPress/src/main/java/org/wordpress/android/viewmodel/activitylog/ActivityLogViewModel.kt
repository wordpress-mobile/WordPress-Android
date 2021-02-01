package org.wordpress.android.viewmodel.activitylog

import androidx.annotation.VisibleForTesting
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import org.wordpress.android.ui.jetpack.restore.usecases.PostRestoreUseCase
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.analytics.ActivityLogTracker
import org.wordpress.android.util.config.ActivityLogFiltersFeatureConfig
import org.wordpress.android.util.config.BackupDownloadFeatureConfig
import org.wordpress.android.util.config.RestoreFeatureConfig
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.util.toFormattedTimeString
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersHidden
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersShown
import java.util.Date
import javax.inject.Inject

const val ACTIVITY_LOG_REWINDABLE_ONLY_KEY = "activity_log_rewindable_only"

private const val DAY_IN_MILLIS = 1000 * 60 * 60 * 24
private const val ONE_SECOND_IN_MILLIS = 1000
private const val TIMEZONE_GMT_0 = "GMT+0"

typealias DateRange = Pair<Long, Long>

/**
 * It was decided to reuse the 'Activity Log' screen instead of creating a new 'Backup' screen. This was due to the
 * fact that there will be lots of code that would need to be duplicated for the new 'Backup' screen. On the other
 * hand, not much more complexity would be introduced if the 'Activity Log' screen is reused (mainly some 'if/else'
 * code branches here and there).
 *
 * However, should more 'Backup' related additions are added to the 'Activity Log' screen, then it should become a
 * necessity to split those features in separate screens in order not to increase further the complexity of this
 * screen's architecture.
 */
class ActivityLogViewModel @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    private val postRestoreUseCase: PostRestoreUseCase,
    private val getRestoreStatusUseCase: GetRestoreStatusUseCase,
    private val resourceProvider: ResourceProvider,
    private val activityLogFiltersFeatureConfig: ActivityLogFiltersFeatureConfig,
    private val backupDownloadFeatureConfig: BackupDownloadFeatureConfig,
    private val dateUtils: DateUtils,
    private val activityLogTracker: ActivityLogTracker,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val restoreFeatureConfig: RestoreFeatureConfig
) : ViewModel() {
    enum class ActivityLogListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    private var isStarted = false

    private val _events = MutableLiveData<List<ActivityLogListItem>>()
    val events: LiveData<List<ActivityLogListItem>>
        get() = _events

    private val _eventListStatus = MutableLiveData<ActivityLogListStatus>()
    val eventListStatus: LiveData<ActivityLogListStatus>
        get() = _eventListStatus

    private val _filtersUiState = MutableLiveData<FiltersUiState>(FiltersHidden)
    val filtersUiState: LiveData<FiltersUiState>
        get() = _filtersUiState

    private val _emptyUiState = MutableLiveData<EmptyUiState>(EmptyUiState.ActivityLog.EmptyFilters)
    val emptyUiState: LiveData<EmptyUiState> = _emptyUiState

    private val _showActivityTypeFilterDialog = SingleLiveEvent<ShowActivityTypePicker>()
    val showActivityTypeFilterDialog: LiveData<ShowActivityTypePicker>
        get() = _showActivityTypeFilterDialog

    private val _showDateRangePicker = SingleLiveEvent<ShowDateRangePicker>()
    val showDateRangePicker: LiveData<ShowDateRangePicker>
        get() = _showDateRangePicker

    private val _moveToTop = SingleLiveEvent<Unit>()
    val moveToTop: SingleLiveEvent<Unit>
        get() = _moveToTop

    private val _showItemDetail = SingleLiveEvent<ActivityLogListItem>()
    val showItemDetail: LiveData<ActivityLogListItem>
        get() = _showItemDetail

    private val _showSnackbarMessage = SingleLiveEvent<String>()
    val showSnackbarMessage: LiveData<String>
        get() = _showSnackbarMessage

    private val _navigationEvents =
            MutableLiveData<Event<ActivityLogNavigationEvents>>()
    val navigationEvents: LiveData<Event<ActivityLogNavigationEvents>>
        get() = _navigationEvents

    private val isRestoreProgressItemShown: Boolean
        get() = events.value?.find { it is ActivityLogListItem.Progress } != null

    private val isDone: Boolean
        get() = eventListStatus.value == ActivityLogListStatus.DONE

    private var fetchActivitiesJob: Job? = null
    private var restoreStatusJob: Job? = null

    private var currentDateRangeFilter: DateRange? = null
    private var currentActivityTypeFilter: List<ActivityTypeModel> = listOf()

    lateinit var site: SiteModel
    var rewindableOnly: Boolean = false

    private var currentRestoreEvent = RestoreEvent(false)

    fun start(site: SiteModel, rewindableOnly: Boolean) {
        if (isStarted) {
            return
        }
        isStarted = true

        this.site = site
        this.rewindableOnly = rewindableOnly

        reloadEvents(true, currentRestoreEvent)
        requestEventsUpdate(false)

        showFiltersIfSupported()
    }

    @VisibleForTesting
    fun reloadEvents(
        done: Boolean = isDone,
        restoreEvent: RestoreEvent
    ) {
        currentRestoreEvent = restoreEvent
        val eventList = activityLogStore.getActivityLogForSite(
                site = site,
                ascending = false,
                rewindableOnly = rewindableOnly
        )
        val items = mutableListOf<ActivityLogListItem>()
        var moveToTop = false
        val withRestoreProgressItem = restoreEvent.displayProgress && !restoreEvent.isCompleted
        if (withRestoreProgressItem) {
            items.add(ActivityLogListItem.Header(resourceProvider.getString(R.string.now)))
            items.add(getRestoreProgressItem(restoreEvent.rewindId, restoreEvent.published))
            moveToTop = eventListStatus.value != ActivityLogListStatus.LOADING_MORE
        }
        eventList.forEach { model ->
            val currentItem = ActivityLogListItem.Event(
                    model,
                    withRestoreProgressItem,
                    backupDownloadFeatureConfig.isEnabled(),
                    restoreFeatureConfig.isEnabled()
            )
            val lastItem = items.lastOrNull() as? ActivityLogListItem.Event
            if (lastItem == null || lastItem.formattedDate != currentItem.formattedDate) {
                items.add(ActivityLogListItem.Header(currentItem.formattedDate))
            }
            items.add(currentItem)
        }
        if (eventList.isNotEmpty() && !done) {
            items.add(ActivityLogListItem.Loading)
        }
        if (eventList.isNotEmpty() && site.hasFreePlan && done) {
            items.add(ActivityLogListItem.Footer)
        }

        _events.value = items
        if (moveToTop) {
            _moveToTop.call()
        }
        if (restoreEvent.isCompleted) {
            showRewindFinishedMessage(restoreEvent.rewindId, restoreEvent.published)
            currentRestoreEvent = RestoreEvent(false)
        }
    }

    private fun getRestoreProgressItem(rewindId: String?, published: Date?): ActivityLogListItem.Progress {
        val rewindDate = published ?: rewindId?.let { activityLogStore.getActivityLogItemByRewindId(it)?.published }
        return rewindDate?.let {
            ActivityLogListItem.Progress(
                    resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                    resourceProvider.getString(
                            R.string.activity_log_currently_restoring_message,
                            rewindDate.toFormattedDateString(),
                            rewindDate.toFormattedTimeString()
                    )
            )
        } ?: ActivityLogListItem.Progress(
                resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                resourceProvider.getString(R.string.activity_log_currently_restoring_message_no_dates)
        )
    }

    private fun showRewindFinishedMessage(rewindId: String?, published: Date?) {
        val rewindDate = published ?: rewindId?.let { activityLogStore.getActivityLogItemByRewindId(it)?.published }
        if (rewindDate != null) {
            _showSnackbarMessage.value =
                    resourceProvider.getString(
                            R.string.activity_log_rewind_finished_snackbar_message,
                            rewindDate.toFormattedDateString(),
                            rewindDate.toFormattedTimeString()
                    )
        } else {
            _showSnackbarMessage.value =
                    resourceProvider.getString(R.string.activity_log_rewind_finished_snackbar_message_no_dates)
        }
    }

    private fun requestEventsUpdate(
        loadMore: Boolean,
        restoreEvent: RestoreEvent = currentRestoreEvent
    ) {
        val isLoadingMore = fetchActivitiesJob != null && eventListStatus.value == ActivityLogListStatus.LOADING_MORE
        val canLoadMore = eventListStatus.value == ActivityLogListStatus.CAN_LOAD_MORE
        if (loadMore && (isLoadingMore || !canLoadMore)) {
            // Ignore loadMore request when already loading more items or there are no more items to load
            return
        }
        fetchActivitiesJob?.cancel()
        val newStatus = if (loadMore) ActivityLogListStatus.LOADING_MORE else ActivityLogListStatus.FETCHING
        _eventListStatus.value = newStatus
        val payload = ActivityLogStore.FetchActivityLogPayload(
                site,
                loadMore,
                currentDateRangeFilter?.first?.let { Date(it) },
                currentDateRangeFilter?.second?.let { Date(it) },
                currentActivityTypeFilter.map { it.key }
        )
        fetchActivitiesJob = viewModelScope.launch {
            val result = activityLogStore.fetchActivities(payload)
            if (isActive) {
                onActivityLogFetched(result, loadMore, restoreEvent)
                fetchActivitiesJob = null
            }
        }
    }

    private fun onActivityLogFetched(
        event: OnActivityLogFetched,
        loadingMore: Boolean,
        restoreEvent: RestoreEvent
    ) {
        if (event.isError) {
            _eventListStatus.value = ActivityLogListStatus.ERROR
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Activity log events")
            return
        }

        if (event.rowsAffected > 0) {
            reloadEvents(
                    done = !event.canLoadMore,
                    restoreEvent = restoreEvent
            )
            if (!loadingMore) {
                moveToTop.call()
            }
            if (!restoreEvent.isCompleted) queryRestoreStatus()
        }

        if (event.canLoadMore) {
            _eventListStatus.value = ActivityLogListStatus.CAN_LOAD_MORE
        } else {
            _eventListStatus.value = ActivityLogListStatus.DONE
        }
    }

    private fun showFiltersIfSupported() {
        when {
            !activityLogFiltersFeatureConfig.isEnabled() -> return
            !site.hasFreePlan -> refreshFiltersUiState()
            else -> {
                viewModelScope.launch {
                    val purchasedProducts = jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId)
                    if (purchasedProducts.backup) {
                        refreshFiltersUiState()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        if (currentDateRangeFilter != null || currentActivityTypeFilter.isNotEmpty()) {
            /**
             * Clear cache when filters are not empty. Filters are not retained across sessions, therefore the data is
             * not relevant when the screen is accessed next time.
             */
            activityLogStore.clearActivityLogCache(site)
        }

        super.onCleared()
    }

    private fun refreshFiltersUiState() {
        val (activityTypeLabel, activityTypeLabelContentDescription) = createActivityTypeFilterLabel()
        val (dateRangeLabel, dateRangeLabelContentDescription) = createDateRangeFilterLabel()
        _filtersUiState.value = FiltersShown(
                dateRangeLabel,
                dateRangeLabelContentDescription,
                activityTypeLabel,
                activityTypeLabelContentDescription,
                currentDateRangeFilter?.let { ::onClearDateRangeFilterClicked },
                currentActivityTypeFilter.takeIf { it.isNotEmpty() }?.let { ::onClearActivityTypeFilterClicked }
        )
        refreshEmptyUiState()
    }

    private fun refreshEmptyUiState() {
        if (rewindableOnly) {
            refreshBackupEmptyUiState()
        } else {
            refreshActivityLogEmptyUiState()
        }
    }

    private fun refreshBackupEmptyUiState() {
        if (currentDateRangeFilter != null) {
            _emptyUiState.value = EmptyUiState.Backup.ActiveFilters
        } else {
            _emptyUiState.value = EmptyUiState.Backup.EmptyFilters
        }
    }

    private fun refreshActivityLogEmptyUiState() {
        if (currentDateRangeFilter != null || currentActivityTypeFilter.isNotEmpty()) {
            _emptyUiState.value = EmptyUiState.ActivityLog.ActiveFilters
        } else {
            _emptyUiState.value = EmptyUiState.ActivityLog.EmptyFilters
        }
    }

    private fun createDateRangeFilterLabel(): kotlin.Pair<UiString, UiString> {
        return currentDateRangeFilter?.let {
            val label = UiStringText(
                    dateUtils.formatDateRange(requireNotNull(it.first), requireNotNull(it.second), TIMEZONE_GMT_0)
            )
            kotlin.Pair(label, label)
        } ?: kotlin.Pair(
                UiStringRes(R.string.activity_log_date_range_filter_label),
                UiStringRes(R.string.activity_log_date_range_filter_label_content_description)
        )
    }

    private fun createActivityTypeFilterLabel(): kotlin.Pair<UiString, UiString> {
        return currentActivityTypeFilter.takeIf { it.isNotEmpty() }?.let {
            if (it.size == 1) {
                kotlin.Pair(
                        UiStringText(it[0].name),
                        UiStringResWithParams(
                                R.string.activity_log_activity_type_filter_single_item_selected_content_description,
                                listOf(UiStringText(it[0].name), UiStringText(it[0].count.toString()))
                        )
                )
            } else {
                kotlin.Pair(
                        UiStringResWithParams(
                                R.string.activity_log_activity_type_filter_active_label,
                                listOf(UiStringText("${it.size}"))
                        ),
                        UiStringResWithParams(
                                R.string.activity_log_activity_type_filter_multiple_items_selected_content_description,
                                listOf(UiStringText("${it.size}"))
                        )
                )
            }
        } ?: kotlin.Pair(
                UiStringRes(R.string.activity_log_activity_type_filter_label),
                UiStringRes(R.string.activity_log_activity_type_filter_no_item_selected_content_description)
        )
    }

    fun onScrolledToBottom() {
        requestEventsUpdate(true)
    }

    fun onPullToRefresh() {
        requestEventsUpdate(false)
    }

    fun onItemClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            _showItemDetail.value = item
        }
    }

    // todo: annmarie - Remove once the feature exclusively uses the more menu
    fun onActionButtonClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            val navigationEvent = if (item.launchRestoreWizard) {
                ActivityLogNavigationEvents.ShowRestore(item)
            } else {
                ActivityLogNavigationEvents.ShowRewindDialog(item)
            }
            _navigationEvents.value = Event(navigationEvent)
        }
    }

    fun onSecondaryActionClicked(
        secondaryAction: ActivityLogListItem.SecondaryAction,
        item: ActivityLogListItem
    ): Boolean {
        if (item is ActivityLogListItem.Event) {
            val navigationEvent = when (secondaryAction) {
                ActivityLogListItem.SecondaryAction.RESTORE -> {
                    if (item.launchRestoreWizard) {
                        ActivityLogNavigationEvents.ShowRestore(item)
                    } else {
                        ActivityLogNavigationEvents.ShowRewindDialog(item)
                    }
                }
                ActivityLogListItem.SecondaryAction.DOWNLOAD_BACKUP -> {
                    ActivityLogNavigationEvents.ShowBackupDownload(item)
                }
            }
            _navigationEvents.value = Event(navigationEvent)
        }
        return true
    }

    fun dateRangePickerClicked() {
        activityLogTracker.trackDateRangeFilterButtonClicked()
        _showDateRangePicker.value = ShowDateRangePicker(initialSelection = currentDateRangeFilter)
    }

    fun onDateRangeSelected(dateRange: DateRange?) {
        val adjustedDateRange = dateRange?.let {
            // adjust time of the end of the date range to 23:59:59
            Pair(dateRange.first, dateRange.second?.let { it + DAY_IN_MILLIS - ONE_SECOND_IN_MILLIS })
        }
        activityLogTracker.trackDateRangeFilterSelected(dateRange)
        currentDateRangeFilter = adjustedDateRange
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onClearDateRangeFilterClicked() {
        activityLogTracker.trackDateRangeFilterCleared()
        currentDateRangeFilter = null
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onActivityTypeFilterClicked() {
        activityLogTracker.trackActivityTypeFilterButtonClicked()
        _showActivityTypeFilterDialog.value = ShowActivityTypePicker(
                RemoteId(site.siteId),
                currentActivityTypeFilter.map { it.key },
                currentDateRangeFilter
        )
    }

    fun onActivityTypesSelected(selectedTypes: List<ActivityTypeModel>) {
        activityLogTracker.trackActivityTypeFilterSelected(selectedTypes)
        currentActivityTypeFilter = selectedTypes
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onClearActivityTypeFilterClicked() {
        activityLogTracker.trackActivityTypeFilterCleared()
        currentActivityTypeFilter = listOf()
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onRestoreConfirmed(rewindId: String) {
        viewModelScope.launch { handleRestoreRequest(postRestoreUseCase.postRestoreRequest(rewindId, site)) }
        showRestoreStartedMessage(rewindId)
    }

    private fun handleRestoreRequest(state: RestoreRequestState) {
        when (state) {
            is RestoreRequestState.Success -> state.restoreId?.let { queryRestoreStatus(it) }
            else -> Unit // Do nothing
        }
    }

    private fun queryRestoreStatus(restoreId: Long? = null) {
        restoreStatusJob?.cancel()
        restoreStatusJob = viewModelScope.launch {
            getRestoreStatusUseCase.getRestoreStatus(site, restoreId)
                    .collect { handleRestoreStatus(it) }
        }
    }

    private fun handleRestoreStatus(state: RestoreRequestState) {
        when (state) {
            is RestoreRequestState.Progress -> if (!isRestoreProgressItemShown) {
                reloadEvents(
                        restoreEvent = RestoreEvent(
                                displayProgress = true,
                                isCompleted = false,
                                rewindId = state.rewindId,
                                published = state.published
                        )
                )
            }
            is RestoreRequestState.Complete -> if (isRestoreProgressItemShown) {
                requestEventsUpdate(
                        loadMore = false,
                        restoreEvent = RestoreEvent(
                                displayProgress = false,
                                isCompleted = true,
                                rewindId = state.rewindId,
                                published = state.published
                        )
                )
            }
            else -> Unit // Do nothing
        }
    }

    private fun showRestoreStartedMessage(rewindId: String) {
        activityLogStore.getActivityLogItemByRewindId(rewindId)?.published?.let {
            _showSnackbarMessage.value = resourceProvider.getString(
                    R.string.activity_log_rewind_started_snackbar_message,
                    it.toFormattedDateString(),
                    it.toFormattedTimeString()
            )
        }
    }

    data class ShowDateRangePicker(val initialSelection: DateRange?)
    data class ShowActivityTypePicker(
        val siteId: RemoteId,
        val initialSelection: List<String>,
        val dateRange: DateRange?
    )

    data class RestoreEvent(
        val displayProgress: Boolean,
        val isCompleted: Boolean = false,
        val rewindId: String? = null,
        val published: Date? = null
    )

    sealed class FiltersUiState(val visibility: Boolean) {
        object FiltersHidden : FiltersUiState(visibility = false)

        data class FiltersShown(
            val dateRangeLabel: UiString,
            val dateRangeLabelContentDescription: UiString,
            val activityTypeLabel: UiString,
            val activityTypeLabelContentDescription: UiString,
            val onClearDateRangeFilterClicked: (() -> Unit)?,
            val onClearActivityTypeFilterClicked: (() -> Unit)?
        ) : FiltersUiState(visibility = true)
    }

    sealed class EmptyUiState {
        abstract val emptyScreenTitle: UiString
        abstract val emptyScreenSubtitle: UiString

        object ActivityLog {
            object EmptyFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.activity_log_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.activity_log_empty_subtitle)
            }

            object ActiveFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.activity_log_active_filter_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.activity_log_active_filter_empty_subtitle)
            }
        }

        object Backup {
            object EmptyFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.backup_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.backup_empty_subtitle)
            }

            object ActiveFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.backup_active_filter_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.backup_active_filter_empty_subtitle)
            }
        }
    }
}
