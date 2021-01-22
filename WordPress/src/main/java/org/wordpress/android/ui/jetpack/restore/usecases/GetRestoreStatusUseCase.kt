package org.wordpress.android.ui.jetpack.restore.usecases

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FAILED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.QUEUED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Complete
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Progress
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

const val DELAY_MILLIS = 10000L

class GetRestoreStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore
) {
    suspend fun getRestoreStatus(
        site: SiteModel,
        restoreId: Long? = null
    ) = flow {
        if (restoreId == null) {
            val result = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(site))
            if (result.isError) {
                emit(RemoteRequestFailure)
                return@flow
            }
        }
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(NetworkUnavailable)
                return@flow
            }

            val rewind = activityLogStore.getRewindStatusForSite(site)?.rewind
            if (rewind != null &&
                    (restoreId == null || rewind.restoreId == restoreId)) {
                when (rewind.status) {
                    FINISHED -> {
                        if (rewind.rewindId != null) {
                            emit(Complete(rewind.rewindId as String, rewind.restoreId))
                        } else {
                            emit(RemoteRequestFailure)
                        }
                        return@flow
                    }
                    FAILED -> {
                        emit(RemoteRequestFailure)
                        return@flow
                    }
                    RUNNING -> {
                        emit(Progress(rewind.rewindId as String, rewind.progress, rewind.message, rewind.currentEntry))
                    }
                    QUEUED -> {
                        emit(Progress(rewind.rewindId as String, rewind.progress, rewind.message, rewind.currentEntry))
                    }
                }
            }

            val result = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(site))
            if (result.isError) {
                emit(RemoteRequestFailure)
                return@flow
            }

            delay(DELAY_MILLIS)
        }
    }
}
