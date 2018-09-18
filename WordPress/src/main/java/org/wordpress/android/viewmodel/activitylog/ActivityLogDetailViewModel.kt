package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel.ActivityActor
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.ui.activitylog.RewindStatusService
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.ACTIVITY_LOG
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.util.toFormattedTimeString
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

const val ACTIVITY_LOG_ID_KEY: String = "activity_log_id_key"
const val ACTIVITY_LOG_REWIND_ID_KEY: String = "activity_log_rewind_id_key"

class ActivityLogDetailViewModel
@Inject constructor(
    val dispatcher: Dispatcher,
    private val activityLogStore: ActivityLogStore,
    private val rewindStatusService: RewindStatusService
) : ViewModel() {
    lateinit var site: SiteModel
    lateinit var activityLogId: String

    private val _showRewindDialog = SingleLiveEvent<ActivityLogDetailModel>()
    val showRewindDialog: LiveData<ActivityLogDetailModel>
        get() = _showRewindDialog

    private val _handleFormattableRangeClick = SingleLiveEvent<FormattableRange>()
    val handleFormattableRangeClick: LiveData<FormattableRange>
        get() = _handleFormattableRangeClick

    private val _item = MutableLiveData<ActivityLogDetailModel>()
    val activityLogItem: LiveData<ActivityLogDetailModel>
        get() = _item

    private val mediatorRewindAvailable = MediatorLiveData<Boolean>()

    val rewindAvailable: LiveData<Boolean>
        get() = mediatorRewindAvailable

    init {
        val itemRewindAvailable = Transformations.map(_item) { it?.isRewindButtonVisible ?: false }
        mediatorRewindAvailable.addSource(itemRewindAvailable) {
            mediatorRewindAvailable.value = it == true && mediatorRewindAvailable.value != false
        }
        mediatorRewindAvailable.addSource(rewindStatusService.rewindAvailable) {
            mediatorRewindAvailable.value = it == true && mediatorRewindAvailable.value != false
        }
    }

    fun start(site: SiteModel, activityLogId: String) {
        this.site = site
        this.activityLogId = activityLogId

        if (activityLogId != _item.value?.activityID) {
            _item.value = activityLogStore
                    .getActivityLogForSite(site)
                    .find { it.activityID == activityLogId }
                    ?.let {
                        ActivityLogDetailModel(
                                activityID = it.activityID,
                                rewindId = it.rewindID,
                                actorIconUrl = it.actor?.avatarURL,
                                showJetpackIcon = it.actor?.showJetpackIcon(),
                                isRewindButtonVisible = it.rewindable ?: false,
                                actorName = it.actor?.displayName,
                                actorRole = it.actor?.role,
                                content = it.content,
                                summary = it.summary,
                                createdDate = it.published.toFormattedDateString(),
                                createdTime = it.published.toFormattedTimeString()
                        )
                    }
        }
        rewindStatusService.start(site)
    }

    fun stop() {
        rewindStatusService.stop()
    }

    fun onRangeClicked(range: FormattableRange) {
        _handleFormattableRangeClick.value = range
    }

    fun onRewindClicked(model: ActivityLogDetailModel) {
        if (model.rewindId != null) {
            _showRewindDialog.value = model
        } else {
            AppLog.e(
                    ACTIVITY_LOG,
                    "Trying to rewind activity without rewind ID"
            )
        }
    }

    private fun ActivityActor.showJetpackIcon(): Boolean {
        return displayName == "Jetpack" && type == "Application" ||
                displayName == "Happiness Engineer" && type == "Happiness Engineer"
    }
}
