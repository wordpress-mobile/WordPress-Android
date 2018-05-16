package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.MutableLiveData
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.Worker.WorkerResult.FAILURE
import androidx.work.Worker.WorkerResult.SUCCESS
import androidx.work.ktx.PeriodicWorkRequestBuilder
import com.helpshift.support.Log
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewind
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import org.wordpress.android.fluxc.store.SiteStore
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

class RewindStatusSyncer
@Inject
constructor(private val activityLogStore: ActivityLogStore, val dispatcher: Dispatcher) {
    private val progressTag = "progressWorkerTag"
    private val mutableRewindStatus = MutableLiveData<RewindStatusModel>()
    private val mutableRewindProgress = MutableLiveData<Rewind>()
    private var site: SiteModel? = null
//
//    val rewindState: LiveData<RewindStatusModel> = mutableRewindStatus
//    val rewindProgress: LiveData<RewindStatusModel.Rewind> = mutableRewindProgress

    fun rewind(rewindId: String, site: SiteModel) {
        dispatcher.dispatch(ActivityLogActionBuilder.newRewindAction(RewindPayload(site, rewindId)))
        val workManager = WorkManager.getInstance()
        if (workManager.getStatusesByTag(progressTag).value != null) {
            Log.d("rewind", "Cancelling worker")
            workManager.cancelAllWorkByTag(progressTag)
        }
        val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NOT_REQUIRED)
                .build()
        val data = Data.Builder().putInt(SITE_ID_KEY, site.id).build()
        val work = PeriodicWorkRequestBuilder<ProgressWorker>(10, SECONDS)
                .setConstraints(networkConstraints)
                .addTag(progressTag)
                .setInputData(data)
                .build()
        Log.d("rewind", "Enqueueing work")
        workManager.enqueue(work)
    }

    fun start(site: SiteModel) {
        this.site = site
        dispatcher.register(this)
        val state = activityLogStore.getRewindStatusForSite(site)
        if (state != null) {
            mutableRewindStatus.postValue(state)
        } else {
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(site)))
        }
    }

    fun stop() {
        dispatcher.unregister(this)
        site = null
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewindStatusFetched(event: OnRewindStatusFetched) {
        Log.d("rewind", "OnRewindStatusFetched")
        site?.let {
            Log.d("rewind", "Site not null")
            val rewindStatusForSite = activityLogStore.getRewindStatusForSite(it)
            rewindStatusForSite?.let {
                Log.d("rewind", "Rewind status for site: ${rewindStatusForSite.state.value}")
                mutableRewindStatus.postValue(it)
            }
            rewindStatusForSite?.rewind?.let {
                Log.d("rewind", "Rewind op state: ${it.status.value}")
                mutableRewindProgress.postValue(it)
                if (it.status != RUNNING) {
                    Log.d("rewind", "Cancelling work on success")
                    WorkManager.getInstance().cancelAllWorkByTag(progressTag)
                }
            }
        }
        if (event.isError) {
            Log.d("rewind", "Cancelling work on error: ${event.error.type}")
//            WorkManager.getInstance().cancelAllWorkByTag(progressTag)
            return
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewind(event: OnRewind) {
        if (event.isError) {
            Log.d("rewind", "OnRewind error: ${event.error.message}")
            WorkManager.getInstance().cancelAllWorkByTag(progressTag)
        }
        Log.d("rewind", "OnRewind: ${event.restoreId}")
    }
}

const val SITE_ID_KEY = "SITE_ID"

class ProgressWorker : Worker() {
    private val counterKey = "COUNTER_KEY"
    @Inject lateinit var activityLogStore: ActivityLogStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var dispatcher: Dispatcher

    override fun doWork(): Worker.WorkerResult {
        Log.d("rewind", "Worker starting")
        (applicationContext as WordPress).component().inject(this)
        val siteId = inputData.getInt(SITE_ID_KEY, -1)
        val counter = inputData.getInt(counterKey, -1)
        Log.d("rewind", "Counter: $counter")
        if (siteId != -1) {
            val site = siteStore.getSiteByLocalId(siteId)
            Log.d("rewind", "Site ID ${site.id}")
            val rewindStatusForSite = activityLogStore.getRewindStatusForSite(site)
            if (rewindStatusForSite?.rewind?.status == FINISHED) {
                Log.d("rewind", "Success")
                return SUCCESS
            }
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(site)))
            Log.d("rewind", "Retrying")
            outputData = Data.Builder().putInt(counterKey, counter + 1).build()
            return WorkerResult.RETRY
        }
        Log.d("rewind", "Failing")
        return FAILURE
    }
}
