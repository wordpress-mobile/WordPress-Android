package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.RewindStatusService
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class ActivityLogViewModel @Inject constructor(
    val dispatcher: Dispatcher,
    private val activityLogStore: ActivityLogStore,
    private val rewindStatusService: RewindStatusService
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

    private val _showItemDetail = SingleLiveEvent<ActivityLogListItem>()
    val showItemDetail: LiveData<ActivityLogListItem>
        get() = _showItemDetail

    private val isRewindInProgress: Boolean
        get() = Transformations.map(
                    rewindStatusService.rewindState,
                    { state -> state.status == RUNNING })
                .value ?: false

    private val isLoadingInProgress: Boolean
        get() = eventListStatus.value == ActivityLogListStatus.LOADING_MORE ||
                eventListStatus.value == ActivityLogListStatus.FETCHING

    private val rewindStateObserver = Observer<RewindStatusModel.Rewind> { state ->
        when (state?.status) {
            RewindStatusModel.Rewind.Status.FINISHED,
            RewindStatusModel.Rewind.Status.FAILED -> reloadEvents()
            else -> {}
        }
    }

    lateinit var site: SiteModel

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)

        rewindStatusService.rewindState.removeObserver(rewindStateObserver)
        rewindStatusService.stop()

        super.onCleared()
    }

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }

        this.site = site

        reloadEvents()
        fetchEvents(false)

        rewindStatusService.start(site)
        rewindStatusService.rewindState.observeForever(rewindStateObserver)

        isStarted = true
    }

    private fun reloadEvents() {
        val eventList = activityLogStore.getActivityLogForSite(site, false)
        val items = eventList.map { model -> ActivityLogListItem.Event(model) }
        _events.postValue(items)
    }

    fun pullToRefresh() {
        fetchEvents(false)
    }

    fun onItemClicked(item: ActivityLogListItem) {
        if (!isRewindInProgress) {
            _showItemDetail.postValue(item)
        }
    }

    fun onRewindButtonClicked(item: ActivityLogListItem) {
        if (!isRewindInProgress) {
            _showRewindDialog.postValue(item)
        }
    }

    fun onRewindConfirmed(rewindActivity: ActivityLogListItem.Progress, rewindId: String) {
        insertProgressListItem(rewindActivity)
        rewindStatusService.rewind(rewindId, site)
    }

    private fun insertProgressListItem(rewindActivity: ActivityLogListItem) {
        val newEvents = ArrayList(_events.value!!)
        newEvents.add(0, rewindActivity)
        _events.postValue(newEvents)
    }

    private fun fetchEvents(loadMore: Boolean) {
        if (shouldFetchEvents(loadMore)) {
            val newStatus = if (loadMore) ActivityLogListStatus.LOADING_MORE else ActivityLogListStatus.FETCHING
            _eventListStatus.postValue(newStatus)
            val payload = ActivityLogStore.FetchActivityLogPayload(site, loadMore)
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchActivitiesAction(payload))
        }
    }

    private fun shouldFetchEvents(loadMore: Boolean): Boolean {
        return when {
            isLoadingInProgress -> false
            loadMore -> _eventListStatus.value == ActivityLogListStatus.CAN_LOAD_MORE
            else -> true
        }
    }

    fun loadMore() {
        fetchEvents(true)
    }

    // Network Callbacks

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onActivityLogFetched(event: OnActivityLogFetched) {
        if (event.isError) {
            _eventListStatus.postValue(ActivityLogListStatus.ERROR)
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Activity log events")
            return
        }

        if (event.rowsAffected > 0) {
            val eventList = activityLogStore.getActivityLogForSite(site, false)
            val items = eventList.map { ActivityLogListItem.Event(it) }
            _events.postValue(items)
        }

        if (event.canLoadMore) {
            _eventListStatus.postValue(ActivityLogListStatus.CAN_LOAD_MORE)
        } else {
            _eventListStatus.postValue(ActivityLogListStatus.DONE)
        }
    }
}
