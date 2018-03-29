package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.support.annotation.WorkerThread
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import javax.inject.Inject

class ActivityLogViewModel() : ViewModel() {
    private lateinit var dispatcher: Dispatcher
    private lateinit var activityLogStore: ActivityLogStore
    private lateinit var dao: ActivityLogDao

    lateinit var events: LiveData<PagedList<ActivityLogModel>>

    var site: SiteModel? = null
        set (value) {
            field = value
            this.dao = ActivityLogDao(value!!, activityLogStore, dispatcher)
            this.events = LivePagedListBuilder(ActivityLogDao.Factory(value, activityLogStore, dispatcher),
                    PagedList.Config.Builder().setPageSize(5).build()).build()
        }

    @Inject
    constructor(dispatcher: Dispatcher, activityLogStore: ActivityLogStore) : this() {
        this.dispatcher = dispatcher
        this.activityLogStore = activityLogStore

        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()

        dispatcher.unregister(this)
    }

    @WorkerThread
    fun fetchActivityLogEntries() {
//        events.postValue(activityLogStore.getEvents())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onActivitiesFetched(event: ActivityLogStore.ActivityError) {
    }
}
