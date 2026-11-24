package org.wordpress.android.ui.media.library

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.media.services.MediaDeleteService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

/**
 * Starts [MediaDeleteService] for the media library and stays bound to it, so the library can queue more deletes
 * and tell which items are still being deleted. One instance per screen: call [release] when the screen goes away.
 */
class MediaDeleteServiceController @Inject constructor(
    private val appContext: Context,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    private var binder: MediaDeleteService.MediaDeleteBinder? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            binder = service as? MediaDeleteService.MediaDeleteBinder
        }

        override fun onServiceDisconnected(className: ComponentName) {
            binder = null
        }
    }

    /**
     * Queues [mediaToDelete] for deletion. With no media this just restarts the service, so it picks up any deletes
     * still pending in the store.
     */
    fun start(site: SiteModel, mediaToDelete: List<MediaModel>?) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start MediaDeleteService, internet connection required.")
            return
        }

        val currentBinder = binder
        if (currentBinder != null) {
            mediaToDelete?.forEach { currentBinder.addMediaToDeleteQueue(it) }
            return
        }

        val intent = Intent(appContext, MediaDeleteService::class.java)
        intent.putExtra(MediaDeleteService.SITE_KEY, site)
        if (mediaToDelete != null) {
            intent.putExtra(MediaDeleteService.MEDIA_LIST_KEY, ArrayList(mediaToDelete))
            if (!isBound) {
                isBound = appContext.bindService(
                    intent,
                    connection,
                    Context.BIND_AUTO_CREATE or Context.BIND_ABOVE_CLIENT
                )
            }
        }
        try {
            appContext.startService(intent)
        } catch (e: IllegalStateException) {
            // This can happen if the app still appears to be running in the background
            // see: https://github.com/wordpress-mobile/WordPress-Android/issues/18638
            AppLog.e(AppLog.T.MEDIA, "Unable to start MediaDeleteService: ${e.message}")
        }
    }

    /**
     * Local ids of the media still queued for deletion. Call on the main thread: the service changes its queue
     * there, and the queue isn't thread-safe.
     */
    fun idsBeingDeleted(): Set<Int> = binder?.service?.deleteQueue?.mapTo(HashSet()) { it.id }.orEmpty()

    fun release() {
        if (isBound) {
            appContext.unbindService(connection)
            isBound = false
        }
        binder = null
    }
}
