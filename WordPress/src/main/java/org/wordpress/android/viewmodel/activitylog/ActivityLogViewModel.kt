package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class ActivityLogViewModel @Inject constructor(
    val dispatcher: Dispatcher,
    private val activityLogStore: ActivityLogStore
) : ViewModel() {
    enum class ActivityLogListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    private var isStarted = false

    private val _events = MutableLiveData<List<ActivityLogListItemViewModel>>()
    val events: LiveData<List<ActivityLogListItemViewModel>>
        get() = _events

    private val _eventListStatus = MutableLiveData<ActivityLogListStatus>()
    val eventListStatus: LiveData<ActivityLogListStatus>
        get() = _eventListStatus

    private val isRewindInProgress: Boolean
        get() = events.value?.isNotEmpty() == true && events.value?.first()?.isRewindInProgress == true

    lateinit var site: SiteModel

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }

        this.site = site

        reloadEvents()
        fetchEvents(false)

        isStarted = true
    }

    private fun reloadEvents() {
        val eventList = activityLogStore.getActivityLogForSite(site, false)
        val items = eventList.map { ActivityLogListItemViewModel.fromDomainModel(it) }
        _events.postValue(items)
    }

    fun pullToRefresh() {
        fetchEvents(false)
    }

    fun onItemClicked(callback: () -> Unit) {
        if (!isRewindInProgress) {
            callback()
        }
    }

    fun onRewindButtonClicked(rewindActivity: ActivityLogListItemViewModel) {
        if (!isRewindInProgress) {
            val newEvents = ArrayList(_events.value!!)
            newEvents.add(0, rewindActivity)
            _events.postValue(newEvents)
        }
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
        return if (_eventListStatus.value == ActivityLogListStatus.FETCHING ||
                _eventListStatus.value == ActivityLogListStatus.LOADING_MORE) {
            false
        } else if (loadMore) {
            _eventListStatus.value == ActivityLogListStatus.CAN_LOAD_MORE
        } else {
            true
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
            val items = eventList.map { ActivityLogListItemViewModel.fromDomainModel(it) }
            _events.postValue(items)
        }

        if (event.canLoadMore) {
            _eventListStatus.postValue(ActivityLogListStatus.CAN_LOAD_MORE)
        } else {
            _eventListStatus.postValue(ActivityLogListStatus.DONE)
        }
    }
}
