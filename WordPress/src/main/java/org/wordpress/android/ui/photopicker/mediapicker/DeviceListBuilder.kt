package org.wordpress.android.ui.photopicker.mediapicker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Video
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.photopicker.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.photopicker.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.photopicker.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.photopicker.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.photopicker.mediapicker.MediaType.VIDEO
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.SqlUtils
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject
import javax.inject.Named

class DeviceListBuilder
@Inject constructor(
    val context: Context,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : MediaSource {
    override suspend fun load(
        mediaTypes: Set<MediaType>,
        offset: Int,
        pageSize: Int?
    ): MediaLoadingResult {
        return withContext(bgDispatcher) {
            val result = mutableListOf<MediaItem>()
            val deferredJobs = mediaTypes.map { mediaType ->
                when(mediaType) {
                    IMAGE -> async { addMedia(Media.EXTERNAL_CONTENT_URI, false) }
                    VIDEO -> async { addMedia(Video.Media.EXTERNAL_CONTENT_URI, true) }
                    AUDIO -> async { addMedia(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true) }
                    DOCUMENT -> TODO()
                }
            }
            deferredJobs.forEach { result.addAll(it.await()) }
            result.sortByDescending { it.id }
            MediaLoadingResult.Success(result, false)
        }
    }

    private fun addMedia(baseUri: Uri, isVideo: Boolean): List<MediaItem> {
        val projection = arrayOf(ID_COL)
        var cursor: Cursor? = null
        val result = mutableListOf<MediaItem>()
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
                val item = MediaItem(
                        id,
                        UriWrapper(Uri.withAppendedPath(baseUri, "" + id)),
                        "",
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
