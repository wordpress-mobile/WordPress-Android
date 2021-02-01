package org.wordpress.android.ui.jetpack.restore.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.QUEUED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.OtherRequestRunning
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

class PostRestoreUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun postRestoreRequest(
        rewindId: String,
        site: SiteModel,
        types: RewindRequestTypes? = null
    ): RestoreRequestState = withContext(ioDispatcher) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return@withContext NetworkUnavailable
        }

        val fetchResult = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(site))
        if (fetchResult.isError) {
            return@withContext RemoteRequestFailure
        }

        val rewind = activityLogStore.getRewindStatusForSite(site)?.rewind
        if (isRestoreRunning(rewind)) {
            return@withContext OtherRequestRunning
        }
        val result = activityLogStore.rewind(RewindPayload(site, rewindId, types))
        if (result.isError) {
            RemoteRequestFailure
        } else {
            if (result.restoreId == null) {
                RemoteRequestFailure
            } else {
                Success(rewindId, result.rewindId, result.restoreId)
            }
        }
    }

    private fun isRestoreRunning(rewind: RewindStatusModel.Rewind?): Boolean {
        if (rewind == null) return false
        return (rewind.status == QUEUED || rewind.status == RUNNING)
    }
}
