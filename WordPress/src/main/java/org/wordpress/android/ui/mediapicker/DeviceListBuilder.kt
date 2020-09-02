package org.wordpress.android.ui.mediapicker

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.net.Uri.parse
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.MediaColumns
import android.provider.MediaStore.Video
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.MimeTypes
import org.wordpress.android.fluxc.utils.MediaUtils
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
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
    private val mimeTypes = MimeTypes()

    override suspend fun load(
        mediaTypes: Set<MediaType>,
        offset: Int,
        pageSize: Int?
    ): MediaLoadingResult {
        return withContext(bgDispatcher) {
            val result = mutableListOf<MediaItem>()
            val deferredJobs = mediaTypes.map { mediaType ->
                when (mediaType) {
                    IMAGE -> async { addMedia(Media.EXTERNAL_CONTENT_URI, IMAGE) }
                    VIDEO -> async { addMedia(Video.Media.EXTERNAL_CONTENT_URI, VIDEO) }
                    AUDIO -> async { addMedia(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, AUDIO) }
                    DOCUMENT -> async { addDownloads() }
                }
            }
            deferredJobs.forEach { result.addAll(it.await()) }
            result.sortByDescending { it.dataModified }
            MediaLoadingResult.Success(result, false)
        }
    }

    private fun addMedia(baseUri: Uri, mediaType: MediaType): List<MediaItem> {
        val projection = arrayOf(ID_COL, ID_DATE_MODIFIED, ID_TITLE)
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
            val dateIndex = cursor.getColumnIndexOrThrow(ID_DATE_MODIFIED)
            val titleIndex = cursor.getColumnIndexOrThrow(ID_TITLE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val dateModified = cursor.getLong(dateIndex)
                val title = cursor.getString(titleIndex)
                val uri = Uri.withAppendedPath(baseUri, "" + id)
                val item = MediaItem(
                        UriWrapper(uri),
                        title,
                        mediaType,
                        getMimeType(uri),
                        dateModified
                )
                if (MediaUtils.isSupportedMimeType(context.contentResolver.getType(uri))) {
                    result.add(item)
                }
            }
        } finally {
            SqlUtils.closeCursor(cursor)
        }
        return result
    }

    private suspend fun addDownloads(): List<MediaItem> = withContext(bgDispatcher) {
        val storagePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return@withContext storagePublicDirectory.listFiles()
                .mapNotNull { file ->
                    val uri = parse(file.toURI().toString())
                    val mimeType = getMimeType(uri)
                    if (mimeType != null && mimeTypes.isSupportedApplicationType(mimeType)) {
                        MediaItem(UriWrapper(uri), file.name, DOCUMENT, mimeType, file.lastModified())
                    } else {
                        null
                    }
                }
    }

    private fun getMimeType(uri: Uri): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            mimeTypes.getMimeTypeForExtension(fileExtension)
        }
    }

    companion object {
        private const val ID_COL = Media._ID
        private const val ID_DATE_MODIFIED = MediaColumns.DATE_MODIFIED
        private const val ID_TITLE = MediaColumns.TITLE
    }
}
