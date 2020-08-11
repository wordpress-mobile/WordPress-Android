package org.wordpress.android.ui.photopicker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Video
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.SqlUtils
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject
import javax.inject.Named

class DeviceMediaListBuilder
@Inject constructor(
    val context: Context,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun buildDeviceMedia(
        browserType: MediaBrowserType
    ): List<PhotoPickerItem> {
        return withContext(bgDispatcher) {
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
            result
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
                        UriWrapper(Uri.withAppendedPath(baseUri, "" + id)),
                        isVideo
                )
                result.add(item)
            }
        } finally {
            SqlUtils.closeCursor(cursor)
        }
        return result
    }

    companion object {
        private const val ID_COL = Media._ID
    }
}
