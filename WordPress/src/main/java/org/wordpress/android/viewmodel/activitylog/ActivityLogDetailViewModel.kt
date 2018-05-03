package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.ui.activitylog.ActivityLogDetailModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

const val ACTIVITY_LOG_ID_KEY: String = "activity_log_id_key"

class ActivityLogDetailViewModel
@Inject constructor(
        val dispatcher: Dispatcher,
        private val activityLogStore: ActivityLogStore
) : ViewModel() {
    lateinit var site: SiteModel
    lateinit var activityLogId: String

    private val _item = MutableLiveData<ActivityLogDetailModel>()
    val activityLogItem: LiveData<ActivityLogDetailModel>
        get() = _item

    fun start(site: SiteModel, activityLogId: String) {
        this.site = site
        this.activityLogId = activityLogId
        if (activityLogId != _item.value?.activityID) {
            _item.postValue(
                    activityLogStore
                            .getActivityLogForSite(site)
                            .find { it.activityID == activityLogId }
                            ?.let {
                                ActivityLogDetailModel(
                                        activityID = it.activityID,
                                        actorIconUrl = it.actor?.avatarURL,
                                        actorName = it.actor?.displayName,
                                        actorRole = it.actor?.role,
                                        text = it.text,
                                        summary = it.summary,
                                        createdDate = it.published.printDate(),
                                        createdTime = it.published.printTime()
                                )
                            }
            )
        }
    }

    private fun Date.printDate(): String {
        return DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(this)
    }

    private fun Date.printTime(): String {
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(this)
    }
}
