package org.wordpress.android.ui.activitylog

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_REWIND_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FAILED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType.GENERIC_ERROR
import org.wordpress.android.modules.DEFAULT_SCOPE
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RewindProgressChecker
@Inject constructor(
    private val activityLogStore: ActivityLogStore,
    @param:Named(DEFAULT_SCOPE) private val defaultScope: CoroutineScope
) {
    companion object {
        const val CHECK_DELAY_MILLIS = 10000L
    }

    suspend fun startNow(site: SiteModel, restoreId: Long): OnRewindStatusFetched? {
        return start(site, restoreId, true)
    }

    suspend fun start(
        site: SiteModel,
        restoreId: Long,
        now: Boolean = false,
        checkDelay: Long = CHECK_DELAY_MILLIS
    ) = withContext(defaultScope.coroutineContext) {
        if (!now) {
            delay(checkDelay)
        }
        var result: OnRewindStatusFetched? = null
        while (coroutineContext.isActive) {
            val rewindStatusForSite = activityLogStore.getRewindStatusForSite(site)
            val rewind = rewindStatusForSite?.rewind
            if (rewind != null && rewind.restoreId == restoreId) {
                if (rewind.status == FINISHED) {
                    result = OnRewindStatusFetched(FETCH_REWIND_STATE)
                    break
                } else if (rewind.status == FAILED) {
                    result = OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR, rewind.reason), FETCH_REWIND_STATE)
                    break
                }
            }
            result = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(site))
            if (result.isError) {
                break
            }
            delay(checkDelay)
        }
        return@withContext result
    }
}
