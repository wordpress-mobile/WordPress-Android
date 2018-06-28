package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.util.Log
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.RewindStatusService
import org.wordpress.android.ui.activitylog.RewindStatusService.RewindProgress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.IActionableItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
import javax.inject.Inject

class ActivityLogViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val activityLogStore: ActivityLogStore,
    private val rewindStatusService: RewindStatusService,
    private val resourceProvider: ResourceProvider
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

    private val _showRewindDialog = SingleLiveEvent<ActivityLogListItem>()
    val showRewindDialog: LiveData<ActivityLogListItem>
        get() = _showRewindDialog

    private val _moveToTop = SingleLiveEvent<Unit>()
    val moveToTop: SingleLiveEvent<Unit>
        get() = _moveToTop

    private val _showItemDetail = SingleLiveEvent<ActivityLogListItem>()
    val showItemDetail: LiveData<ActivityLogListItem>
        get() = _showItemDetail

    private val isLoadingInProgress: Boolean
        get() = eventListStatus.value == ActivityLogListStatus.LOADING_MORE ||
                eventListStatus.value == ActivityLogListStatus.FETCHING

    private val isRewindProgressItemShown: Boolean
        get() = _events.value?.getOrNull(0) is ActivityLogListItem.Progress

    private var areActionsEnabled: Boolean = true

    private var lastRewindActivityId: String? = null
    private var lastRewindStatus: Status? = null
    private val rewindProgressObserver = Observer<RewindProgress> {
        Log.d("rewind_service", "Rewind progress: $it")
        if (it?.activityLogItem?.activityID != lastRewindActivityId || it?.status != lastRewindStatus) {
            lastRewindActivityId = it?.activityLogItem?.activityID
            updateRewindState(it?.status, it?.activityLogItem?.activityID != null)
        }
    }

    private fun updateRewindState(status: Status?, hasActivity: Boolean) {
        lastRewindStatus = status
        Log.d("rewind_service", "Status: $status, is in progress: $isRewindProgressItemShown")
        if (status == RUNNING && !isRewindProgressItemShown) {
            reloadEvents(true, true)
        } else if (status != RUNNING && isRewindProgressItemShown) {
            requestEventsUpdate(false)
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

    init {
        dispatcher.register(this)
    }

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }

        this.site = site

        rewindStatusService.start(site)
        rewindStatusService.rewindProgress.observeForever(rewindProgressObserver)
        rewindStatusService.rewindAvailable.observeForever(rewindAvailableObserver)

        activityLogStore.getRewindStatusForSite(site)

        reloadEvents()
        requestEventsUpdate(false)

        isStarted = true
    }

    override fun onCleared() {
        dispatcher.unregister(this)

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
            _showItemDetail.postValue(item)
        }
    }

    fun onActionButtonClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            _showRewindDialog.postValue(item)
        }
    }

    fun onRewindConfirmed(rewindId: String) {
        rewindStatusService.rewind(rewindId, site)
    }

    fun onScrolledToBottom() {
        requestEventsUpdate(true)
    }

    private fun reloadEvents(disableActions: Boolean = areActionsEnabled,
        displayProgressItem: Boolean = isRewindProgressItemShown) {
        val eventList = activityLogStore.getActivityLogForSite(site, false)
        Log.d("rewind_service", "Reloading: disable actions: $disableActions, progress item: $displayProgressItem")
        val items = ArrayList<ActivityLogListItem>(eventList.map { model -> ActivityLogListItem.Event(model) })
        areActionsEnabled = true

        if (disableActions) {
            disableListActions(items.filter { it is IActionableItem }.map { it as IActionableItem })
        }

        if (displayProgressItem) {
            insertRewindProgressItem(items)
            moveToTop()
        }

        prepareHeaders(items)

        _events.postValue(items)
    }

    private fun moveToTop() {
        if (eventListStatus.value != LOADING_MORE) {
            _moveToTop.asyncCall()
        }
    }

    private fun prepareHeaders(items: List<ActivityLogListItem>) {
        items.forEachIndexed { i, _ ->
            if (i == 0 || items[i - 1].header != items[i].header) {
                items[i].isHeaderVisible = true
            }
        }
    }

    private fun insertRewindProgressItem(items: ArrayList<ActivityLogListItem>) {
        val activityLogModel = rewindStatusService.rewindProgress.value?.activityLogItem
        items.add(0, getRewindProgressItem(activityLogModel))
    }

    private fun disableListActions(items: List<IActionableItem>) {
        areActionsEnabled = false
        items.forEach {
            it.isButtonVisible = false
        }
    }

    private fun getRewindProgressItem(activityLogModel: ActivityLogModel?): ActivityLogListItem.Progress {
        return activityLogModel?.let {
            val rewoundEvent = ActivityLogListItem.Event(it)
            ActivityLogListItem.Progress(resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                    resourceProvider.getString(R.string.activity_log_currently_restoring_message,
                            rewoundEvent.formattedDate, rewoundEvent.formattedTime),
                    resourceProvider.getString(R.string.now))
        } ?: ActivityLogListItem.Progress(resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                resourceProvider.getString(R.string.activity_log_currently_restoring_message_no_dates),
                resourceProvider.getString(R.string.now))
    }

    private fun requestEventsUpdate(isLoadingMore: Boolean) {
        if (canRequestEventsUpdate(isLoadingMore)) {
            val newStatus = if (isLoadingMore) ActivityLogListStatus.LOADING_MORE else ActivityLogListStatus.FETCHING
            _eventListStatus.postValue(newStatus)
            val payload = ActivityLogStore.FetchActivityLogPayload(site, isLoadingMore)
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchActivitiesAction(payload))
        }
    }

    private fun canRequestEventsUpdate(isLoadingMore: Boolean): Boolean {
        return when {
            isLoadingInProgress -> false
            isLoadingMore -> _eventListStatus.value == ActivityLogListStatus.CAN_LOAD_MORE
            else -> true
        }
    }

    // Network Callbacks

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onEventsUpdated(event: OnActivityLogFetched) {
        if (event.isError) {
            _eventListStatus.postValue(ActivityLogListStatus.ERROR)
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Activity log events")
            return
        }

        if (event.rowsAffected > 0) {
            reloadEvents(!rewindStatusService.isRewindAvailable, rewindStatusService.isRewindInProgress)
            rewindStatusService.requestStatusUpdate()
            moveToTop()
        }

        if (event.canLoadMore) {
            _eventListStatus.postValue(ActivityLogListStatus.CAN_LOAD_MORE)
        } else {
            _eventListStatus.postValue(ActivityLogListStatus.DONE)
        }
    }
}
