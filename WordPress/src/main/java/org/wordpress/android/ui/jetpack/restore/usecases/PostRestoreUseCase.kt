package org.wordpress.android.ui.jetpack.restore.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
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
        types: RewindRequestTypes
    ): RestoreRequestState = withContext(ioDispatcher) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            NetworkUnavailable
        }

        val result = activityLogStore.rewind(RewindPayload(site, rewindId, types))
        if (result.isError) {
            RemoteRequestFailure
        } else {
            if (result.rewindId == rewindId) {
                if (result.restoreId == null) {
                    RemoteRequestFailure
                } else {
                    Success(rewindId, result.rewindId, result.restoreId)
                }
            } else {
                OtherRequestRunning
            }
        }
    }
}
