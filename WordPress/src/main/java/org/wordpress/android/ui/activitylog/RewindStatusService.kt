package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewind
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import java.util.Date
import javax.inject.Inject

class RewindStatusService
@Inject
constructor(
    private val activityLogStore: ActivityLogStore,
    private val workerController: RewindStateProgressWorkerController,
    private val dispatcher: Dispatcher
) {
    private val mutableRewindAvailable = MutableLiveData<Boolean>()
    private val mutableRewindError = MutableLiveData<RewindError>()
    private val mutableRewindStatusFetchError = MutableLiveData<RewindStatusError>()
    private val mutableRewindState = MutableLiveData<Rewind>()
    private val mutableRewindProgress = MutableLiveData<RewindProgress>()
    private var site: SiteModel? = null

    val rewindAvailable: LiveData<Boolean> = mutableRewindAvailable
    val rewindError: LiveData<RewindError> = mutableRewindError
    val rewindStatusFetchError: LiveData<RewindStatusError> = mutableRewindStatusFetchError
    val rewindState: LiveData<Rewind> = mutableRewindState
    val rewindProgress: LiveData<RewindProgress> = mutableRewindProgress

    fun rewind(rewindId: String, site: SiteModel) {
        dispatcher.dispatch(ActivityLogActionBuilder.newRewindAction(RewindPayload(site, rewindId)))
        updateRewindProgress(rewindId, 0, RUNNING)
        mutableRewindAvailable.postValue(false)
        mutableRewindError.postValue(null)
        mutableRewindState.postValue(null)
    }

    fun start(site: SiteModel) {
        this.site = site
        dispatcher.register(this)
        if (!reloadRewindStatus()) {
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(site)))
        }
    }

    fun stop() {
        dispatcher.unregister(this)
        site = null
    }

    private fun reloadRewindStatus(): Boolean {
        site?.let {
            val state = activityLogStore.getRewindStatusForSite(it)
            state?.let {
                updateRewindStatus(state)
                return true
            }
        }
        return false
    }

    private fun updateRewindStatus(rewindStatus: RewindStatusModel?) {
        mutableRewindAvailable.postValue(
                rewindStatus != null &&
                        rewindStatus.state == ACTIVE &&
                        rewindStatus.rewind?.status != RUNNING
        )
        val rewind = rewindStatus?.rewind
        mutableRewindState.postValue(rewind)
        if (rewind != null) {
            rewind.rewindId?.let { rewindId ->
                updateRewindProgress(rewindId, rewind.progress, rewind.status, rewind.reason)
            }
            if (rewind.status != RUNNING) {
                workerController.cancelWorker()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewindStatusFetched(event: OnRewindStatusFetched) {
        mutableRewindStatusFetchError.postValue(event.error)
        if (event.isError) {
            workerController.cancelWorker()
        }
        reloadRewindStatus()
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewind(event: OnRewind) {
        mutableRewindError.postValue(event.error)
        if (event.isError) {
            mutableRewindAvailable.postValue(true)
            reloadRewindStatus()
            updateRewindProgress(event.rewindId, 0, Status.FAILED, event.error?.type?.toString())
            return
        }
        site?.let {
            event.restoreId?.let { restoreId ->
                workerController.startWorker(it, restoreId)
            }
        }
    }

    private fun updateRewindProgress(
        rewindId: String,
        progress: Int?,
        rewindStatus: Rewind.Status,
        rewindError: String? = null
    ) {
        activityLogStore.getActivityLogItemByRewindId(rewindId)?.let {
            val rewindProgress = RewindProgress(it.activityID, progress, it.published, rewindStatus, rewindError)
            mutableRewindProgress.postValue(rewindProgress)
        }
    }

    data class RewindProgress(
        val activityId: String,
        val progress: Int?,
        val date: Date,
        val status: Rewind.Status,
        val failureReason: String? = null
    )
}
