package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
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
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.util.analytics.AnalyticsUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RewindStatusService
@Inject constructor(
    private val activityLogStore: ActivityLogStore,
    private val rewindProgressChecker: RewindProgressChecker,
    @param:Named(UI_SCOPE) private val uiScope: CoroutineScope
) {
    private val mutableRewindAvailable = MutableLiveData<Boolean>()
    private val mutableRewindError = MutableLiveData<RewindError>()
    private val mutableRewindStatusFetchError = MutableLiveData<RewindStatusError>()
    private val mutableRewindProgress = MutableLiveData<RewindProgress>()
    private var site: SiteModel? = null
    private var activityLogModelItem: ActivityLogModel? = null
    private var rewindProgressCheckerJob: Job? = null
    private var fetchRewindJob: Job? = null

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

    fun rewind(rewindId: String, site: SiteModel) = uiScope.launch {
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.ACTIVITY_LOG_REWIND_STARTED,
                site, mutableMapOf(REWIND_ID_TRACKING_KEY to rewindId as Any))

        updateRewindProgress(rewindId, 0, RUNNING)
        mutableRewindAvailable.value = false
        mutableRewindError.value = null

        val rewindResult = activityLogStore.rewind(RewindPayload(site, rewindId))
        onRewind(rewindResult)
    }

    fun start(site: SiteModel) {
        if (this.site == null) {
            this.site = site
            requestStatusUpdate()
            reloadRewindStatus()
        }
    }

    fun stop() {
        rewindProgressCheckerJob?.cancel()
        fetchRewindJob?.cancel()
        if (site != null) {
            site = null
        }
    }

    fun requestStatusUpdate() {
        site?.let {
            fetchRewindJob?.cancel()
            fetchRewindJob = uiScope.launch {
                val rewindStatus = activityLogStore.fetchActivitiesRewind(FetchRewindStatePayload(it))
                onRewindStatusFetched(rewindStatus.error, rewindStatus.isError)
            }
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
        mutableRewindAvailable.value = rewindStatus?.state == ACTIVE && rewindStatus.rewind?.status != RUNNING

        val rewind = rewindStatus?.rewind
        if (rewind != null) {
            val restoreId = rewindStatus.rewind?.restoreId
            if (rewindProgressCheckerJob?.isActive != true && restoreId != null) {
                site?.let {
                    rewindProgressCheckerJob = uiScope.launch {
                        val rewindStatusFetched = rewindProgressChecker.startNow(it, restoreId)
                        onRewindStatusFetched(rewindStatusFetched?.error, rewindStatusFetched?.isError == true)
                    }
                }
            }
            updateRewindProgress(rewind.rewindId, rewind.progress, rewind.status, rewind.reason)
            if (rewind.status != RUNNING) {
                rewindProgressCheckerJob?.cancel()
            }
        } else {
            mutableRewindProgress.setValue(null)
        }
    }

    private fun onRewindStatusFetched(rewindStatusError: RewindStatusError?, isError: Boolean) {
        mutableRewindStatusFetchError.value = rewindStatusError
        if (isError) {
            rewindProgressCheckerJob?.cancel()
        }
        reloadRewindStatus()
    }

    private fun onRewind(event: OnRewind) {
        mutableRewindError.value = event.error
        if (event.isError) {
            mutableRewindAvailable.value = true
            reloadRewindStatus()
            updateRewindProgress(event.rewindId, 0, Status.FAILED, event.error?.type?.toString())
            return
        }
        site?.let {
            event.restoreId?.let { restoreId ->
                rewindProgressCheckerJob = uiScope.launch {
                    val rewindStatusFetched = rewindProgressChecker.start(it, restoreId)
                    onRewindStatusFetched(rewindStatusFetched?.error, rewindStatusFetched?.isError == true)
                }
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
        mutableRewindProgress.value = rewindProgress
    }

    data class RewindProgress(
        val activityLogItem: ActivityLogModel?,
        val progress: Int?,
        val date: Date?,
        val status: Rewind.Status,
        val failureReason: String? = null
    )
}
