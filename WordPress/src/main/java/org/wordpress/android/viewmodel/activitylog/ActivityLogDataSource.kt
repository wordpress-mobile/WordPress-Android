package org.wordpress.android.viewmodel.activitylog

import android.arch.paging.DataSource
import android.util.Log
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import java.util.Date
import java.util.UUID

class ActivityLogDataSource (val site: SiteModel, val store: ActivityLogStore, val dispatcher: Dispatcher) : BaseDataSource<ActivityLogModel>() {
    val arrayList = ArrayList<ActivityLogModel>()

    override fun countItems(): Int {
        return arrayList.size
    }

    override fun loadRangeAtPosition(position: Int, size: Int): List<ActivityLogModel>? {
        val payload = ActivityLogStore.FetchActivityLogPayload(site, position, size)
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
//        dispatcher.dispatch(action)

        Log.d("Onko", "position $position, size $size")

        for (a in 1..size) {
            arrayList.add(ActivityLogModel(UUID.randomUUID().toString(), "Summary", "Text", "Name $a", "Type", "x", "Status", true, "1", Date(), false, ActivityLogModel.ActivityActor("ActorName", "ActorType", 123, null, "Admin")))
        }
        return arrayList
     }

    class Factory(val site: SiteModel, val store: ActivityLogStore, val dispatcher: Dispatcher) : DataSource.Factory<Int, ActivityLogModel>() {
        override fun create(): DataSource<Int, ActivityLogModel> {
            return ActivityLogDataSource(site, store, dispatcher)
        }
    }
}
