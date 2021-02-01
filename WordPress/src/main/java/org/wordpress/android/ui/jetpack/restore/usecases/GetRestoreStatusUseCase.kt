package org.wordpress.android.ui.jetpack.restore.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FAILED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.QUEUED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Complete
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Progress
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

const val DELAY_MILLIS = 5000L

class GetRestoreStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun getRestoreStatus(
        site: SiteModel,
        restoreId: Long? = null
    ) = flow {
        if (restoreId == null) {
            if (!isNetworkAvailable()) return@flow
            if (!fetchActivitiesRewind(site)) return@flow
        }
        while (true) {
            if (!isNetworkAvailable()) return@flow

            val rewind = activityLogStore.getRewindStatusForSite(site)?.rewind
            if (rewind != null &&
                    (restoreId == null || rewind.restoreId == restoreId)) {
                when (rewind.status) {
                    FINISHED -> {
                        emitFinished(rewind)
                        return@flow
                    }
                    FAILED -> {
                        emitFailure()
                        return@flow
                    }
                    RUNNING -> emitProgress(rewind)
                    QUEUED -> emitProgress(rewind)
                }
            }

            if (!fetchActivitiesRewind(site)) return@flow

            delay(DELAY_MILLIS)
        }
    }.flowOn(bgDispatcher)

    private suspend fun FlowCollector<RestoreRequestState>.isNetworkAvailable(): Boolean {
        return if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(NetworkUnavailable)
            false
        } else true
    }

    private suspend fun FlowCollector<RestoreRequestState>.fetchActivitiesRewind(site: SiteModel): Boolean {
        val result = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(site))
        return if (result.isError) {
            emit(RemoteRequestFailure)
            false
        } else true
    }

    private suspend fun FlowCollector<RestoreRequestState>.emitFinished(rewind: Rewind) =
            if (rewind.rewindId != null) emitComplete(rewind) else emitFailure()

    private suspend fun FlowCollector<RestoreRequestState>.emitComplete(rewind: Rewind) {
        val rewindId = rewind.rewindId as String
        val published = activityLogStore.getActivityLogItemByRewindId(rewindId)?.published
        emit(Complete(rewind.rewindId as String, rewind.restoreId, published))
    }

    private suspend fun FlowCollector<RestoreRequestState>.emitFailure() = emit(RemoteRequestFailure)

    private suspend fun FlowCollector<RestoreRequestState>.emitProgress(rewind: Rewind) {
        val rewindId = rewind.rewindId as String
        val published = activityLogStore.getActivityLogItemByRewindId(rewindId)?.published
        emit(Progress(rewindId, rewind.progress, rewind.message, rewind.currentEntry, published))
    }
}
