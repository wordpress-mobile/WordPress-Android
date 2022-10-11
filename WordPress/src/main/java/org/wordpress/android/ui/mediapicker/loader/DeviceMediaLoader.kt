package org.wordpress.android.ui.mediapicker.loader

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.MediaColumns
import android.provider.MediaStore.Video
import android.webkit.MimeTypeMap
import org.wordpress.android.fluxc.utils.MimeTypes
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.SqlUtils
import org.wordpress.android.util.UriWrapper
import java.io.File
import javax.inject.Inject

class DeviceMediaLoader
@Inject constructor(private val context: Context, private val localeManagerWrapper: LocaleManagerWrapper) {
    private val mimeTypes = MimeTypes()
    fun loadMedia(
        mediaType: MediaType,
        filter: String?,
        pageSize: Int,
        limitDate: Long? = null
    ): DeviceMediaList {
        val baseUri = when (mediaType) {
            IMAGE -> Media.EXTERNAL_CONTENT_URI
            VIDEO -> Video.Media.EXTERNAL_CONTENT_URI
            AUDIO -> Audio.Media.EXTERNAL_CONTENT_URI
            else -> throw IllegalArgumentException("Cannot load media for selected type $mediaType")
        }
        val result = mutableListOf<DeviceMediaItem>()
        val projection = arrayOf(ID_COL, ID_DATE_MODIFIED, ID_TITLE)
        var cursor: Cursor? = null
        val dateCondition = if (limitDate != null && limitDate != 0L) {
            "$ID_DATE_MODIFIED <= \'$limitDate\'"
        } else {
            null
        }
        val filterCondition = filter?.let { "$ID_TITLE LIKE \'%$filter%\'" }
        val condition = if (dateCondition != null && filterCondition != null) {
            "$dateCondition AND $filterCondition"
        } else {
            dateCondition ?: filterCondition
        }

        cursor = getCursor(condition, pageSize, baseUri, projection)

        if (cursor == null) {
            return DeviceMediaList(listOf(), null)
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
                val item = DeviceMediaItem(
                        UriWrapper(uri),
                        title,
                        dateModified
                )
                result.add(item)
            }
        } finally {
            SqlUtils.closeCursor(cursor)
        }

        val nextItem = if (result.size > pageSize) {
            result.last().dateModified
        } else {
            null
        }
        return DeviceMediaList(result.take(pageSize), nextItem)
    }

    private fun getCursor(
        condition: String?,
        pageSize: Int,
        baseUri: Uri,
        projection: Array<String>
    ) = if (VERSION.SDK_INT >= VERSION_CODES.Q /*29*/) {
        val bundle = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, condition)
            putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(FileColumns.DATE_MODIFIED)
            )
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize + 1)
            putInt(ContentResolver.QUERY_ARG_OFFSET, 0)
        }
        context.contentResolver.query(
                baseUri,
                projection,
                bundle,
                null
        )
    } else {
        context.contentResolver.query(
                baseUri,
                projection,
                condition,
                null,
                "$ID_DATE_MODIFIED DESC LIMIT ${(pageSize + 1)}"
        )
    }

    @Suppress("DEPRECATION")
    fun loadDocuments(filter: String?, pageSize: Int, limitDate: Long? = null): DeviceMediaList {
        val storagePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val nextPage = (storagePublicDirectory?.listFiles() ?: arrayOf()).filter {
            (limitDate == null || it.lastModifiedInSecs() <= limitDate) && (filter == null || it.name.toLowerCase(
                    localeManagerWrapper.getLocale()
            ).contains(filter))
        }.sortedByDescending { it.lastModified() }.take(pageSize + 1)

        val nextItem = if (nextPage.size > pageSize) {
            nextPage.last().lastModifiedInSecs()
        } else {
            null
        }
        val result = nextPage.take(pageSize).map { file ->
            val uri = Uri.parse(file.toURI().toString())
            DeviceMediaItem(
                    UriWrapper(uri),
                    file.name,
                    file.lastModifiedInSecs()
            )
        }
        return DeviceMediaList(result, nextItem)
    }

    private fun File.lastModifiedInSecs() = this.lastModified() / 1000

    fun getMimeType(uri: UriWrapper): String? {
        return if (uri.uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.getType(uri.uri)
        } else {
            val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri.uri.toString())
            mimeTypes.getMimeTypeForExtension(fileExtension)
        }
    }

    data class DeviceMediaList(val items: List<DeviceMediaItem>, val next: Long? = null)

    data class DeviceMediaItem(val uri: UriWrapper, val title: String, val dateModified: Long)

    companion object {
        private const val ID_COL = Media._ID
        private const val ID_DATE_MODIFIED = MediaColumns.DATE_MODIFIED
        private const val ID_TITLE = MediaColumns.TITLE
    }
}
