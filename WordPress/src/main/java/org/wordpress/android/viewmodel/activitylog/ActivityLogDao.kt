package org.wordpress.android.viewmodel.activitylog

import android.arch.paging.PositionalDataSource
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import java.util.Date

class ActivityLogDao (val site: SiteModel, val store: ActivityLogStore, val dispatcher: Dispatcher) : PositionalDataSource<ActivityLogModel>() {
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ActivityLogModel>) {
        val payload = ActivityLogStore.FetchActivityLogPayload(site, params.startPosition, params.loadSize)
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
//        dispatcher.dispatch(action)

        callback.onResult(listOf(ActivityLogModel("1", "Summary", "Text", "Name", "Type", "x", "Status", true, "1", Date(), false, ActivityLogModel.ActivityActor("ActorName", "ActorType", 123, null, "Admin"))))
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ActivityLogModel>) {
        val payload = ActivityLogStore.FetchActivityLogPayload(site, params.requestedStartPosition, params.requestedLoadSize)
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
//        dispatcher.dispatch(action)

        callback.onResult(listOf(ActivityLogModel("1", "Summary", "Text", "Name", "Type", "x", "Status", true, "1", Date(), false, ActivityLogModel.ActivityActor("ActorName", "ActorType", 123, null, "Admin"))), 0, 1)
    }
}
