package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.os.Handler
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.PluginBrowserViewModel
import javax.inject.Inject

class ActivityLogViewModel @Inject constructor(val dispatcher: Dispatcher, private val activityLogStore: ActivityLogStore) : ViewModel() {
    enum class ActivityLogListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    private var isStarted = false

    private val handler = Handler()

    private val _events = MutableLiveData<List<ActivityLogModel>>()
    val events: LiveData<List<ActivityLogModel>>
        get() = _events


    private val _eventListStatus = MutableLiveData<ActivityLogListStatus>()
    val eventListStatus: LiveData<ActivityLogListStatus>
        get() = _eventListStatus

    var site: SiteModel? = null

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun writeToBundle(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, site)
    }

    fun readFromBundle(savedInstanceState: Bundle) {
        if (isStarted) {
            // This was called due to a config change where the data survived, we don't need to
            // read from the bundle
            return
        }
        site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
    }

    fun start() {
        if (isStarted) {
            return
        }

        reloadEvents()
        fetchEvents(false)

        isStarted = true
    }

    private fun reloadEvents() {
        site?.let {
            val eventList = activityLogStore.getActivityLogForSite(site!!)
            _events.postValue(eventList)
        }
    }

    fun pullToRefresh() {
        fetchEvents(false)
    }

    private fun fetchEvents(loadMore: Boolean) {
        if (!shouldFetchEvents(loadMore)) {
            return
        }
        val newStatus = if (loadMore) ActivityLogListStatus.LOADING_MORE else ActivityLogListStatus.FETCHING
        _eventListStatus.postValue(newStatus)
        val payload = ActivityLogStore.FetchActivityLogPayload(site!!, 10, 0)
        dispatcher.dispatch(ActivityLogActionBuilder.newFetchActivitiesAction(payload))
    }

    private fun shouldFetchEvents(loadMore: Boolean): Boolean {
        if (_eventListStatus == ActivityLogListStatus.FETCHING || _eventListStatus == ActivityLogListStatus.LOADING_MORE) {
            // if we are already fetching something we shouldn't start a new one. Even if we are loading more plugins
            // and the user pulled to refresh, we don't want (or need) the 2 requests colliding
            return false
        }
        return !(loadMore && _eventListStatus != PluginBrowserViewModel.PluginListStatus.CAN_LOAD_MORE)
    }

    fun loadMore() {
        fetchEvents(true)
    }

    // Network Callbacks

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onWPOrgPluginFetched(event: ActivityLogStore.OnActivityLogFetched) {
        if (event.isError) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching the Activity log events")
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (event.rowsAffected > 0 && event.activityLogModels != null) {
            updateEventListIfNecessary(event.activityLogModels!!)
        }
    }

    private fun updateEventListIfNecessary(list: List<ActivityLogModel>) {
        handler.postDelayed({
            // Using the size of the set for comparison might fail since we clear the updatedPluginSlugSet
            _events.postValue(list)
        }, 250)
    }
}
