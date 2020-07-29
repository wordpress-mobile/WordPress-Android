package org.wordpress.android.ui.photopicker

import android.content.Context
import android.os.AsyncTask
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.BuildDeviceMediaListTask.BuildDeviceMediaListListener
import javax.inject.Inject

class DeviceMediaListBuilder
@Inject constructor(val context: Context) {
    fun buildDeviceMedia(mustReload: Boolean, mBrowserType: MediaBrowserType, callback: BuildDeviceMediaListListener) {
        BuildDeviceMediaListTask(
                mustReload,
                mBrowserType,
                context,
                callback
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}
