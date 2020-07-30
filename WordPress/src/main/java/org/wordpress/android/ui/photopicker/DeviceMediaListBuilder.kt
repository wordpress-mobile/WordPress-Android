package org.wordpress.android.ui.photopicker

import android.content.Context
import android.os.AsyncTask
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.BuildDeviceMediaListTask.BuildDeviceMediaListListener
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class DeviceMediaListBuilder
@Inject constructor(val context: Context) {
    private fun buildDeviceMedia(
        browserType: MediaBrowserType,
        callback: BuildDeviceMediaListListener
    ) {
        BuildDeviceMediaListTask(
                browserType,
                context,
                callback
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    suspend fun buildDeviceMedia(browserType: MediaBrowserType) =
            suspendCancellableCoroutine<List<PhotoPickerItem>> { cont ->
                buildDeviceMedia(browserType, object : BuildDeviceMediaListListener {
                    override fun onCancelled() {
                        cont.cancel()
                    }

                    override fun onSuccess(result: List<PhotoPickerItem>) {
                        (cont as Continuation<List<PhotoPickerItem>>).resume(result)
                    }
                })
            }
}
