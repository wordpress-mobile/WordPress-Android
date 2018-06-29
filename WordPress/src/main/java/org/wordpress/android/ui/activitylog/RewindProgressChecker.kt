package org.wordpress.android.ui.activitylog

import android.os.Handler
import android.os.Looper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewindProgressChecker
@Inject
constructor(val activityLogStore: ActivityLogStore, val siteStore: SiteStore, val dispatcher: Dispatcher) {
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var checkState: Runnable? = null
    var isRunning: Boolean = false

    companion object {
        const val CHECK_DELAY_MILLIS = 10000L
    }

    fun startNow(site: SiteModel, restoreId: Long) {
        start(site, restoreId, true)
    }

    fun start(site: SiteModel, restoreId: Long, now: Boolean = false) {
        isRunning = true
        checkState = object : Runnable {
            override fun run() {
                val rewindStatusForSite = activityLogStore.getRewindStatusForSite(site)
                val rewind = rewindStatusForSite?.rewind
                rewind?.let {
                    if (rewind.status == FINISHED && rewind.restoreId == restoreId) {
                        isRunning = false
                        return
                    }
                }

                val action = ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(site))
                dispatcher.dispatch(action)

                handler.postDelayed(this, CHECK_DELAY_MILLIS)
            }
        }
        if (now) {
            handler.post(checkState)
        } else {
            handler.postDelayed(checkState, CHECK_DELAY_MILLIS)
        }
    }

    fun cancel() {
        checkState?.let {
            handler.removeCallbacks(checkState)
            isRunning = false
        }
    }
}
