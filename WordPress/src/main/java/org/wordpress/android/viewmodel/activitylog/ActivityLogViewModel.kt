package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListStateLiveDataDelegate
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class ActivityLogViewModel @Inject constructor(
    val dispatcher: Dispatcher,
    private val activityLogStore: ActivityLogStore
) : ViewModel() {
    val events = ListStateLiveDataDelegate<ActivityLogListItemViewModel>()
    var eventListState: ListState<ActivityLogListItemViewModel> by events

    private var isStarted = false

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
        eventListState = ListState.Ready(items)
    }

    fun pullToRefresh() {
        fetchEvents(false)
    }

    private fun fetchEvents(loadMore: Boolean) {
        if (eventListState.shouldFetch(loadMore)) {
            eventListState = ListState.Loading(eventListState, loadMore)
            val payload = ActivityLogStore.FetchActivityLogPayload(site, loadMore)
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchActivitiesAction(payload))
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
            eventListState = ListState.Error(eventListState, event.error.message)
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Activity log events")
            return
        }

        if (event.rowsAffected > 0) {
            val eventList = activityLogStore.getActivityLogForSite(site, false)
            val items = eventList.map { ActivityLogListItemViewModel.fromDomainModel(it) }
            eventListState = ListState.Success(items, event.canLoadMore)
        }
    }
}
