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
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Empty
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Progress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

const val DELAY_MILLIS = 1000L
const val MAX_RETRY = 3
const val DELAY_FACTOR = 2

class GetRestoreStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val tag = javaClass.simpleName
    @Suppress("ComplexMethod", "LoopWithTooManyJumpStatements")
    suspend fun getRestoreStatus(
        site: SiteModel,
        restoreId: Long? = null
    ) = flow {
        var retryAttempts = 0
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                val retryAttemptsExceeded = handleError(retryAttempts++, NetworkUnavailable)
                if (retryAttemptsExceeded) break else continue
            }
            if (!fetchActivitiesRewind(site)) {
                val retryAttemptsExceeded = handleError(retryAttempts++, RemoteRequestFailure)
                if (retryAttemptsExceeded) break else continue
            }

            retryAttempts = 0
            val rewind = activityLogStore.getRewindStatusForSite(site)?.rewind
            if (rewind == null) {
                emit(Empty)
                break
            }
            if (restoreId == null || rewind.restoreId == restoreId) {
                when (rewind.status) {
                    FINISHED -> {
                        emitFinished(rewind)
                        break
                    }
                    FAILED -> {
                        emitFailure()
                        break
                    }
                    RUNNING -> emitProgress(rewind)
                    QUEUED -> emitProgress(rewind)
                }
            }
        }
    }.flowOn(bgDispatcher)

    private suspend fun fetchActivitiesRewind(site: SiteModel): Boolean {
        val result = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(site))
        return !result.isError
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

    private suspend fun FlowCollector<RestoreRequestState>.handleError(retryAttempts: Int, failure: Failure): Boolean {
        return if (retryAttempts >= MAX_RETRY) {
            AppLog.d(T.JETPACK_BACKUP, "$tag: Exceeded $MAX_RETRY retries while fetching status")
            emit(failure)
            true
        } else {
            delay(DELAY_MILLIS * (max(1, DELAY_FACTOR * retryAttempts)))
            false
        }
    }
}
