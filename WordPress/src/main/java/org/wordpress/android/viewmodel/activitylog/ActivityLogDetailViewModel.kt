package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.ActivityLogStore
import javax.inject.Inject

class ActivityLogDetailViewModel() : ViewModel() {
    private lateinit var dispatcher: Dispatcher
    private lateinit var activityLogStore: ActivityLogStore

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
}
