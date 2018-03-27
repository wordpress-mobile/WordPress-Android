package org.wordpress.android.viewmodel.activitylog

import android.arch.paging.PositionalDataSource
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore

class ActivityLogDao (val site: SiteModel, val store: ActivityLogStore, val dispatcher: Dispatcher) : PositionalDataSource<ActivityLogModel>() {
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ActivityLogModel>) {
        val payload = ActivityLogStore.FetchActivitiesPayload(site, params.startPosition, params.loadSize)
        val action = ActivityActionBuilder.newFetchActivitiesAction(payload)
        dispatcher.dispatch(action)
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ActivityLogModel>) {
        val payload = ActivityLogStore.FetchActivitiesPayload(site, params.requestedStartPosition, params.requestedLoadSize)
        val action = ActivityActionBuilder.newFetchActivitiesAction(payload)
        dispatcher.dispatch(action)
    }
}
