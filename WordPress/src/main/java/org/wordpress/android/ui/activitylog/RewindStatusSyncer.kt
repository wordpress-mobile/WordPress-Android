package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.work.Constraints.Builder
import androidx.work.Data
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.Worker.WorkerResult.FAILURE
import androidx.work.Worker.WorkerResult.SUCCESS
import androidx.work.ktx.PeriodicWorkRequestBuilder
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
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
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
    private val mutableRewindAvailable = MutableLiveData<Boolean>()
    private val mutableRewindState = MutableLiveData<Rewind>()
    private var site: SiteModel? = null

    val rewindAvailable: LiveData<Boolean> = mutableRewindAvailable
    val rewindState: LiveData<Rewind> = mutableRewindState

    fun rewind(rewindId: String, site: SiteModel) {
        dispatcher.dispatch(ActivityLogActionBuilder.newRewindAction(RewindPayload(site, rewindId)))
        mutableRewindAvailable.postValue(false)
        mutableRewindState.postValue(null)
        RewindStateProgressWorker.startWorker(site)
    }

    fun start(site: SiteModel) {
        this.site = site
        dispatcher.register(this)
        val state = activityLogStore.getRewindStatusForSite(site)
        if (state != null) {
            onRewindStatus(state)
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
        if (event.isError) {
            RewindStateProgressWorker.cancelWorker()
            return
        }
        site?.let {
            val rewindStatusForSite = activityLogStore.getRewindStatusForSite(it)
            rewindStatusForSite?.let {
                onRewindStatus(it)
                it.rewind?.let {
                    mutableRewindState.postValue(it)
                    if (it.status != RUNNING) {
                        RewindStateProgressWorker.cancelWorker()
                    }
                }
            }
        }
    }

    private fun onRewindStatus(rewindStatus: RewindStatusModel?) {
        mutableRewindAvailable.postValue(rewindStatus != null && rewindStatus.state == ACTIVE && rewindStatus.rewind == null)
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewind(event: OnRewind) {
        if (event.isError) {
            RewindStateProgressWorker.cancelWorker()
        }
        site?.let {
            val state = activityLogStore.getRewindStatusForSite(it)
            if (state != null) {
                onRewindStatus(state)
            }
        }
    }
}

const val SITE_ID_KEY = "SITE_ID"

class RewindStateProgressWorker : Worker() {
    companion object {
        const val TAG = "progressWorkerTag"
        fun startWorker(site: SiteModel) {
            val workManager = WorkManager.getInstance()
            if (workManager.getStatusesByTag(RewindStateProgressWorker.TAG).value != null) {
                workManager.cancelAllWorkByTag(RewindStateProgressWorker.TAG)
            }
            val networkConstraints = Builder()
                    .setRequiredNetworkType(NOT_REQUIRED)
                    .build()
            val data = Data.Builder().putInt(SITE_ID_KEY, site.id).build()
            val work = PeriodicWorkRequestBuilder<RewindStateProgressWorker>(10, SECONDS)
                    .setConstraints(networkConstraints)
                    .addTag(RewindStateProgressWorker.TAG)
                    .setInputData(data)
                    .build()
            workManager.enqueue(work)
        }

        fun cancelWorker() {
            WorkManager.getInstance().cancelAllWorkByTag(RewindStateProgressWorker.TAG)
        }
    }
    @Inject
    lateinit var activityLogStore: ActivityLogStore
    @Inject
    lateinit var siteStore: SiteStore
    @Inject
    lateinit var dispatcher: Dispatcher

    override fun doWork(): Worker.WorkerResult {
        (applicationContext as WordPress).component().inject(this)
        val siteId = inputData.getInt(SITE_ID_KEY, -1)
        if (siteId != -1) {
            val site = siteStore.getSiteByLocalId(siteId)
            val rewindStatusForSite = activityLogStore.getRewindStatusForSite(site)
            if (rewindStatusForSite?.rewind?.status == FINISHED) {
                return SUCCESS
            }
            dispatcher.dispatch(ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(site)))
            return WorkerResult.RETRY
        }
        return FAILURE
    }

    class
}
