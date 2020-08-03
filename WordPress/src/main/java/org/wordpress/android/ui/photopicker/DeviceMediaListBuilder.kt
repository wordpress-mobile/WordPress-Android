package org.wordpress.android.ui.photopicker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Video
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.SqlUtils
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class DeviceMediaListBuilder
@Inject constructor(
    val context: Context,
    @param:Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher
    fun buildDeviceMedia(
        browserType: MediaBrowserType,
        callback: BuildDeviceMediaListListener
    ) {
        launch {
            val result = mutableListOf<PhotoPickerItem>()
            // images
            if (browserType.isImagePicker) {
                result.addAll(addMedia(Media.EXTERNAL_CONTENT_URI, false))
            }

            // videos
            if (browserType.isVideoPicker) {
                result.addAll(addMedia(Video.Media.EXTERNAL_CONTENT_URI, true))
            }
            result.sortByDescending { it.id }
            withContext(uiDispatcher) {
                callback.onSuccess(result)
            }
        }
    }

    private fun addMedia(baseUri: Uri, isVideo: Boolean): List<PhotoPickerItem> {
        val projection = arrayOf(ID_COL)
        var cursor: Cursor? = null
        val result = mutableListOf<PhotoPickerItem>()
        try {
            cursor = context.contentResolver.query(
                    baseUri,
                    projection,
                    null,
                    null,
                    null
            )
        } catch (e: SecurityException) {
            AppLog.e(MEDIA, e)
        }
        if (cursor == null) {
            return result
        }
        try {
            val idIndex = cursor.getColumnIndexOrThrow(ID_COL)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val item = PhotoPickerItem(
                        id,
                        Uri.withAppendedPath(baseUri, "" + id),
                        isVideo
                )
                result.add(item)
            }
        } finally {
            SqlUtils.closeCursor(cursor)
        }
        return result
    }

    interface BuildDeviceMediaListListener {
        fun onSuccess(result: List<PhotoPickerItem>?)
    }

    companion object {
        private const val ID_COL = Media._ID
    }
}
