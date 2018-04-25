package org.wordpress.android.viewmodel.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import javax.inject.Inject

const val ACTIVITY_LOG_ID_KEY: String = "activity_log_id_key"

class ActivityLogDetailViewModel
@Inject constructor(
        val dispatcher: Dispatcher,
        private val activityLogStore: ActivityLogStore
) : ViewModel() {
    lateinit var site: SiteModel
    lateinit var activityLogId: String

    private val _item = MutableLiveData<ActivityLogModel>()
    val activityLogItem: LiveData<ActivityLogModel>
        get() = _item

    enum class ActivityLogDetailStatus {
        DONE,
        ERROR,
        FETCHING
    }


    fun writeToBundle(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, site)
        outState.putString(ACTIVITY_LOG_ID_KEY, activityLogId)
    }

    fun readFromIntent(intent: Intent) {
        site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
        activityLogId = intent.getStringExtra(ACTIVITY_LOG_ID_KEY)
    }

    fun readFromBundle(bundle: Bundle) {
        site = bundle.getSerializable(WordPress.SITE) as SiteModel
        activityLogId = bundle.getString(ACTIVITY_LOG_ID_KEY)
    }

    fun start() {
        _item.postValue(activityLogStore
                .getActivityLogForSite(site, true)
                .find { it.activityID == activityLogId })
    }
}
