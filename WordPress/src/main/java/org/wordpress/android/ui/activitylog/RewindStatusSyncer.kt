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
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewind
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import javax.inject.Inject

class RewindStatusSyncer
@Inject
constructor(
    private val activityLogStore: ActivityLogStore,
    private val workerController: RewindStateProgressWorkerController,
    private val dispatcher: Dispatcher
) {
    private val mutableRewindAvailable = MutableLiveData<Boolean>()
    private val mutableRewindState = MutableLiveData<Rewind>()
    private var site: SiteModel? = null

    val rewindAvailable: LiveData<Boolean> = mutableRewindAvailable
    val rewindState: LiveData<Rewind> = mutableRewindState

    fun rewind(rewindId: String, site: SiteModel) {
        dispatcher.dispatch(ActivityLogActionBuilder.newRewindAction(RewindPayload(site, rewindId)))
        mutableRewindAvailable.postValue(false)
        mutableRewindState.postValue(null)
        workerController.startWorker(site)
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

    private fun onRewindStatus(rewindStatus: RewindStatusModel?) {
        mutableRewindAvailable.postValue(
                rewindStatus != null &&
                        rewindStatus.state == ACTIVE &&
                        rewindStatus.rewind?.status != RUNNING
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewindStatusFetched(event: OnRewindStatusFetched) {
        if (event.isError) {
            workerController.cancelWorker()
            return
        }
        site?.let {
            val rewindStatusForSite = activityLogStore.getRewindStatusForSite(it)
            rewindStatusForSite?.let {
                onRewindStatus(it)
                it.rewind?.let {
                    mutableRewindState.postValue(it)
                    if (it.status != RUNNING) {
                        workerController.cancelWorker()
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onRewind(event: OnRewind) {
        if (event.isError) {
            workerController.cancelWorker()
        }
        site?.let {
            val state = activityLogStore.getRewindStatusForSite(it)
            if (state != null) {
                onRewindStatus(state)
            }
        }
    }
}
