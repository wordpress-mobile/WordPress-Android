package org.wordpress.android.ui.activitylog

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_REWIND_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.coroutineContext

@Singleton
class RewindProgressChecker
@Inject constructor(private val activityLogStore: ActivityLogStore) {
    companion object {
        const val CHECK_DELAY_MILLIS = 10000L
    }

    suspend fun startNow(site: SiteModel, restoreId: Long): OnRewindStatusFetched? {
        return start(site, restoreId, true)
    }

    suspend fun start(site: SiteModel, restoreId: Long, now: Boolean = false) = runBlocking(coroutineContext) {
        if (!now) {
            delay(CHECK_DELAY_MILLIS)
        }
        var result: OnRewindStatusFetched? = null
        while(isActive) {
            val rewindStatusForSite = activityLogStore.getRewindStatusForSite(site)
            val rewind = rewindStatusForSite?.rewind
            if (rewind != null && rewind.status == FINISHED && rewind.restoreId == restoreId) {
                result = OnRewindStatusFetched(FETCH_REWIND_STATE)
                break
            }
            result = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(site))
            if (result.isError) {
                break
            }
            delay(CHECK_DELAY_MILLIS)
        }
        result
    }
}
