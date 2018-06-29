package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
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
import org.wordpress.android.util.AnalyticsUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewindStatusService
@Inject
constructor(
    private val activityLogStore: ActivityLogStore,
    private val rewindProgressChecker: RewindProgressChecker,
    private val dispatcher: Dispatcher
) {
    private val mutableRewindAvailable = MutableLiveData<Boolean>()
    private val mutableRewindError = MutableLiveData<RewindError>()
    private val mutableRewindStatusFetchError = MutableLiveData<RewindStatusError>()
    private val mutableRewindProgress = MutableLiveData<RewindProgress>()
    private var site: SiteModel? = null
    private var activityLogModelItem: ActivityLogModel? = null

    val rewindingActivity: ActivityLogModel?
        get() = activityLogModelItem

    val rewindAvailable: LiveData<Boolean> = mutableRewindAvailable
    val rewindError: LiveData<RewindError> = mutableRewindError
    val rewindStatusFetchError: LiveData<RewindStatusError> = mutableRewindStatusFetchError
    val rewindProgress: LiveData<RewindProgress> = mutableRewindProgress

    val isRewindInProgress: Boolean
        get() = rewindProgress.value?.status == Status.RUNNING

    val isRewindAvailable: Boolean
        get() = rewindAvailable.value == true

    companion object {
        const val REWIND_ID_TRACKING_KEY = "rewind_id"
    }

    fun rewind(rewindId: String, site: SiteModel) {
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STARTED_ACTIVITY_LOG_REWIND,
                site, mutableMapOf(REWIND_ID_TRACKING_KEY to rewindId as Any))

        dispatcher.dispatch(ActivityLogActionBuilder.newRewindAction(RewindPayload(site, rewindId)))
        updateRewindProgress(rewindId, 0, RUNNING)
        mutableRewindAvailable.postValue(false)
        mutableRewindError.postValue(null)
    }

    fun start(site: SiteModel) {
        if (this.site == null) {
            this.site = site
            dispatcher.register(this)
            requestStatusUpdate()
            reloadRewindStatus()
        }
    }

    fun stop() {
        if (site != null) {
            dispatcher.unregister(this)
            site = null
        }
    }

    fun requestStatusUpdate() {
        site?.let {
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(it)))
        }
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
        mutableRewindAvailable.postValue(rewindStatus?.state == ACTIVE && rewindStatus.rewind?.status != RUNNING)

        val rewind = rewindStatus?.rewind
        if (rewind != null) {
            val restoreId = rewindStatus.rewind?.restoreId
            if (!rewindProgressChecker.isRunning && restoreId != null) {
                site?.let { rewindProgressChecker.startNow(it, restoreId) }
            }
            updateRewindProgress(rewind.rewindId, rewind.progress, rewind.status, rewind.reason)
            if (rewind.status != RUNNING) {
                rewindProgressChecker.cancel()
            }
        } else {
            mutableRewindProgress.postValue(null)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewindStatusFetched(event: OnRewindStatusFetched) {
        mutableRewindStatusFetchError.postValue(event.error)
        if (event.isError) {
            rewindProgressChecker.cancel()
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
                rewindProgressChecker.start(it, restoreId)
            }
        }
    }

    private fun updateRewindProgress(
        rewindId: String?,
        progress: Int?,
        rewindStatus: Rewind.Status,
        rewindError: String? = null
    ) {
        var activityItem = if (rewindId != null) activityLogStore.getActivityLogItemByRewindId(rewindId) else null
        if (activityItem == null && activityLogModelItem != null && activityLogModelItem?.rewindID == rewindId) {
            activityItem = activityLogModelItem
        }
        if (activityItem != null) {
            activityLogModelItem = activityItem
        }
        val rewindProgress = RewindProgress(
                activityItem,
                progress,
                activityItem?.published,
                rewindStatus,
                rewindError
        )
        mutableRewindProgress.postValue(rewindProgress)
    }

    data class RewindProgress(
        val activityLogItem: ActivityLogModel?,
        val progress: Int?,
        val date: Date?,
        val status: Rewind.Status,
        val failureReason: String? = null
    )
}
