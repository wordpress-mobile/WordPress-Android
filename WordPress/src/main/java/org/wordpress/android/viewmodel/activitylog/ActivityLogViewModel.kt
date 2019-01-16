package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.activitylog.RewindStatusService
import org.wordpress.android.ui.activitylog.RewindStatusService.RewindProgress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Footer
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Header
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Loading
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.DONE
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
import javax.inject.Inject
import javax.inject.Named

class ActivityLogViewModel @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    private val rewindStatusService: RewindStatusService,
    private val resourceProvider: ResourceProvider,
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

    private val _showRewindDialog = SingleLiveEvent<ActivityLogListItem>()
    val showRewindDialog: LiveData<ActivityLogListItem>
        get() = _showRewindDialog

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
        get() = eventListStatus.value == ActivityLogListStatus.LOADING_MORE ||
                eventListStatus.value == ActivityLogListStatus.FETCHING

    private val isRewindProgressItemShown: Boolean
        get() = _events.value?.containsProgressItem() == true

    private val isDone: Boolean
        get() = eventListStatus.value == DONE

    private var areActionsEnabled: Boolean = true

    private var lastRewindActivityId: String? = null
    private var lastRewindStatus: Status? = null
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

        isStarted = true
    }

    override fun onCleared() {
        rewindStatusService.rewindAvailable.removeObserver(rewindAvailableObserver)
        rewindStatusService.rewindProgress.removeObserver(rewindProgressObserver)
        rewindStatusService.stop()

        super.onCleared()
    }

    fun onPullToRefresh() {
        requestEventsUpdate(false)
    }

    fun onItemClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            _showItemDetail.value = item
        }
    }

    fun onActionButtonClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            _showRewindDialog.value = item
        }
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
            reloadEvents(true, true)
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
            items.add(Header(resourceProvider.getString(string.now)))
            items.add(getRewindProgressItem(activityLogModel))
            moveToTop = eventListStatus.value != LOADING_MORE
        }
        eventList.forEach { model ->
            val currentItem = ActivityLogListItem.Event(model, disableActions)
            val lastItem = items.lastOrNull() as? Event
            if (lastItem == null || lastItem.formattedDate != currentItem.formattedDate) {
                items.add(ActivityLogListItem.Header(currentItem.formattedDate))
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
            val rewoundEvent = ActivityLogListItem.Event(it)
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
            val event = Event(it)
            _showSnackbarMessage.value = resourceProvider.getString(
                    string.activity_log_rewind_started_snackbar_message,
                    event.formattedDate,
                    event.formattedTime
            )
        }
    }

    private fun showRewindFinishedMessage() {
        val item = rewindStatusService.rewindingActivity
        if (item != null) {
            val event = Event(item)
            _showSnackbarMessage.value =
                    resourceProvider.getString(string.activity_log_rewind_finished_snackbar_message,
                            event.formattedDate,
                            event.formattedTime)
        } else {
            _showSnackbarMessage.value =
                    resourceProvider.getString(string.activity_log_rewind_finished_snackbar_message_no_dates)
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
            _eventListStatus.value = ActivityLogListStatus.DONE
        }
    }
}
