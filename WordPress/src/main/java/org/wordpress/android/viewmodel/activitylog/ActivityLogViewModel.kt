package org.wordpress.android.viewmodel.activitylog

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.rewind.RewindStatusService
import org.wordpress.android.ui.jetpack.rewind.RewindStatusService.RewindProgress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Footer
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Header
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Loading
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BackupFeatureConfig
import org.wordpress.android.util.config.ActivityLogFiltersFeatureConfig
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.DONE
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersHidden
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersShown
import javax.inject.Inject
import javax.inject.Named

typealias DateRange = Pair<Long, Long>

class ActivityLogViewModel @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    private val rewindStatusService: RewindStatusService,
    private val resourceProvider: ResourceProvider,
    private val activityLogFiltersFeatureConfig: ActivityLogFiltersFeatureConfig,
    private val backupFeatureConfig: BackupFeatureConfig,
    @param:Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
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

    private val _filtersUiState = MutableLiveData<FiltersUiState>()
    val filtersUiState: LiveData<FiltersUiState>
        get() = _filtersUiState

    private val _showRewindDialog = SingleLiveEvent<ActivityLogListItem>()
    val showRewindDialog: LiveData<ActivityLogListItem>
        get() = _showRewindDialog

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

    private val isLoadingInProgress: Boolean
        get() = eventListStatus.value == LOADING_MORE ||
                eventListStatus.value == ActivityLogListStatus.FETCHING

    private val isRewindProgressItemShown: Boolean
        get() = _events.value?.containsProgressItem() == true

    private val isDone: Boolean
        get() = eventListStatus.value == DONE

    private var areActionsEnabled: Boolean = true

    private var lastRewindActivityId: String? = null
    private var lastRewindStatus: Status? = null

    private var currentDateRangeFilter: DateRange? = null
    private var currentActivityTypeFilter: List<Int> = listOf()

    private val rewindProgressObserver = Observer<RewindProgress> {
        if (it?.activityLogItem?.activityID != lastRewindActivityId || it?.status != lastRewindStatus) {
            lastRewindActivityId = it?.activityLogItem?.activityID
            updateRewindState(it?.status)
        }
    }

    private val rewindAvailableObserver = Observer<Boolean> { isRewindAvailable ->
        if (areActionsEnabled != isRewindAvailable) {
            isRewindAvailable?.let {
                reloadEvents(!isRewindAvailable)
            }
        }
    }

    private val _showMoreMenu = SingleLiveEvent<ActivityLogListItem>()
    val showMoreMenu: LiveData<ActivityLogListItem>
        get() = _showMoreMenu

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }

        this.site = site

        rewindStatusService.start(site)
        rewindStatusService.rewindProgress.observeForever(rewindProgressObserver)
        rewindStatusService.rewindAvailable.observeForever(rewindAvailableObserver)

        activityLogStore.getRewindStatusForSite(site)

        reloadEvents(done = true)
        requestEventsUpdate(false)

        refreshFiltersUiState()

        isStarted = true
    }

    override fun onCleared() {
        rewindStatusService.rewindAvailable.removeObserver(rewindAvailableObserver)
        rewindStatusService.rewindProgress.removeObserver(rewindProgressObserver)
        rewindStatusService.stop()

        super.onCleared()
    }

    private fun refreshFiltersUiState() {
        _filtersUiState.value = if (activityLogFiltersFeatureConfig.isEnabled()) {
            FiltersShown(
                    UiStringRes(R.string.activity_log_date_range_filter_label),
                    UiStringRes(R.string.activity_log_activity_type_filter_label)
            )
        } else {
            FiltersHidden
        }
    }

    fun onPullToRefresh() {
        requestEventsUpdate(false)
    }

    fun onItemClicked(item: ActivityLogListItem) {
        if (item is Event) {
            _showItemDetail.value = item
        }
    }

    fun onActionButtonClicked(item: ActivityLogListItem) {
        if (item is Event) {
            if (backupFeatureConfig.isEnabled()) {
                // todo: annmarie implement expand/dismiss the more menu
                _showMoreMenu.value = item
            } else {
                _showRewindDialog.value = item
            }
        }
    }

    fun dateRangePickerClicked() {
        _showDateRangePicker.value = ShowDateRangePicker(initialSelection = currentDateRangeFilter)
    }

    fun onDateRangeSelected(dateRange: DateRange?) {
        currentDateRangeFilter = dateRange
        // TODO malinjir: refetch/load data
    }

    fun onActivityTypeFilterClicked() {
        // TODO malinjir pass initially selected activity types
        _showActivityTypeFilterDialog.value = ShowActivityTypePicker(RemoteId(site.siteId), currentActivityTypeFilter)
    }

    fun onActivityTypesSelected(activityTypeIds: List<Int>) {
        currentActivityTypeFilter = activityTypeIds
        // TODO malinjir: refetch/load data
    }

    fun onRewindConfirmed(rewindId: String) {
        rewindStatusService.rewind(rewindId, site)
        showRewindStartedMessage()
    }

    fun onScrolledToBottom() {
        requestEventsUpdate(true)
    }

    private fun updateRewindState(status: Status?) {
        lastRewindStatus = status
        if (status == RUNNING && !isRewindProgressItemShown) {
            reloadEvents(disableActions = true, displayProgressItem = true)
        } else if (status != RUNNING && isRewindProgressItemShown) {
            requestEventsUpdate(false)
        }
    }

    private fun reloadEvents(
        disableActions: Boolean = areActionsEnabled,
        displayProgressItem: Boolean = isRewindProgressItemShown,
        done: Boolean = isDone
    ) {
        val eventList = activityLogStore.getActivityLogForSite(site, false)
        val items = mutableListOf<ActivityLogListItem>()
        var moveToTop = false
        val rewindFinished = isRewindProgressItemShown && !displayProgressItem
        if (displayProgressItem) {
            val activityLogModel = rewindStatusService.rewindProgress.value?.activityLogItem
            items.add(Header(resourceProvider.getString(R.string.now)))
            items.add(getRewindProgressItem(activityLogModel))
            moveToTop = eventListStatus.value != LOADING_MORE
        }
        eventList.forEach { model ->
            val currentItem = Event(model, disableActions, backupFeatureConfig.isEnabled())
            val lastItem = items.lastOrNull() as? Event
            if (lastItem == null || lastItem.formattedDate != currentItem.formattedDate) {
                items.add(Header(currentItem.formattedDate))
            }
            items.add(currentItem)
        }
        if (eventList.isNotEmpty() && !done) {
            items.add(Loading)
        }
        if (eventList.isNotEmpty() && site.hasFreePlan && done) {
            items.add(Footer)
        }
        areActionsEnabled = !disableActions

        _events.value = items
        if (moveToTop) {
            _moveToTop.call()
        }
        if (rewindFinished) {
            showRewindFinishedMessage()
        }
    }

    private fun List<ActivityLogListItem>.containsProgressItem(): Boolean {
        return this.find { it is ActivityLogListItem.Progress } != null
    }

    private fun getRewindProgressItem(activityLogModel: ActivityLogModel?): ActivityLogListItem.Progress {
        return activityLogModel?.let {
            val rewoundEvent = ActivityLogListItem.Event(
                    model = it,
                    backupFeatureEnabled = backupFeatureConfig.isEnabled())
            ActivityLogListItem.Progress(resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                    resourceProvider.getString(R.string.activity_log_currently_restoring_message,
                            rewoundEvent.formattedDate, rewoundEvent.formattedTime))
        } ?: ActivityLogListItem.Progress(resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                resourceProvider.getString(R.string.activity_log_currently_restoring_message_no_dates))
    }

    private fun requestEventsUpdate(isLoadingMore: Boolean) {
        if (canRequestEventsUpdate(isLoadingMore)) {
            val newStatus = if (isLoadingMore) ActivityLogListStatus.LOADING_MORE else ActivityLogListStatus.FETCHING
            _eventListStatus.value = newStatus
            val payload = ActivityLogStore.FetchActivityLogPayload(site, isLoadingMore)
            launch {
                val result = activityLogStore.fetchActivities(payload)
                onActivityLogFetched(result, isLoadingMore)
            }
        }
    }

    private fun canRequestEventsUpdate(isLoadingMore: Boolean): Boolean {
        return when {
            isLoadingInProgress -> false
            isLoadingMore -> _eventListStatus.value == ActivityLogListStatus.CAN_LOAD_MORE
            else -> true
        }
    }

    private fun showRewindStartedMessage() {
        rewindStatusService.rewindingActivity?.let {
            val event = Event(model = it, backupFeatureEnabled = backupFeatureConfig.isEnabled())
            _showSnackbarMessage.value = resourceProvider.getString(
                    R.string.activity_log_rewind_started_snackbar_message,
                    event.formattedDate,
                    event.formattedTime
            )
        }
    }

    private fun showRewindFinishedMessage() {
        val item = rewindStatusService.rewindingActivity
        if (item != null) {
            val event = Event(model = item, backupFeatureEnabled = backupFeatureConfig.isEnabled())
            _showSnackbarMessage.value =
                    resourceProvider.getString(R.string.activity_log_rewind_finished_snackbar_message,
                            event.formattedDate,
                            event.formattedTime)
        } else {
            _showSnackbarMessage.value =
                    resourceProvider.getString(R.string.activity_log_rewind_finished_snackbar_message_no_dates)
        }
    }

    private fun onActivityLogFetched(event: OnActivityLogFetched, loadingMore: Boolean) {
        if (event.isError) {
            _eventListStatus.value = ActivityLogListStatus.ERROR
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Activity log events")
            return
        }

        if (event.rowsAffected > 0) {
            reloadEvents(
                    !rewindStatusService.isRewindAvailable,
                    rewindStatusService.isRewindInProgress,
                    !event.canLoadMore
            )
            if (!loadingMore) {
                moveToTop.call()
            }
            rewindStatusService.requestStatusUpdate()
        }

        if (event.canLoadMore) {
            _eventListStatus.value = ActivityLogListStatus.CAN_LOAD_MORE
        } else {
            _eventListStatus.value = DONE
        }
    }

    data class ShowDateRangePicker(val initialSelection: DateRange?)
    data class ShowActivityTypePicker(val siteId: RemoteId, val initialSelection: List<Int>)

    sealed class FiltersUiState(val visibility: Boolean) {
        object FiltersHidden : FiltersUiState(visibility = false)

        data class FiltersShown(
            val dateRangeLabel: UiString,
            val activityTypeLabel: UiString
        ) : FiltersUiState(visibility = true)
    }
}
