package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import javax.inject.Inject

class ActivityLogViewModel() : ViewModel() {
    private lateinit var dispatcher: Dispatcher
    private lateinit var activityLogStore: ActivityLogStore

    private lateinit var activityLogModel: ActivityLogModel

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
