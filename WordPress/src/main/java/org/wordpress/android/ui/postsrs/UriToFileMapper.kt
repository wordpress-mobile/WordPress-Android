package org.wordpress.android.ui.postsrs

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class UriToFileMapper @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    @Suppress("Recycle") // stream is closed by .use {}
    fun copyUriToTempFile(uri: Uri): File {
        val mime = appContext.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mime)
            ?.let { ".$it" }
            ?: ".jpg"
        val tempFile = File.createTempFile(
            "featured_img_",
            ext,
            appContext.cacheDir
        )
        try {
            val inputStream = appContext.contentResolver
                .openInputStream(uri)
                ?: throw IOException("Cannot read URI")
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            if (!tempFile.delete()) {
                AppLog.w(
                    AppLog.T.MEDIA,
                    "Failed to delete temp file: ${tempFile.name}"
                )
            }
            throw e
        }
        return tempFile
    }

    @Suppress("Recycle") // cursor is closed by .use {}
    fun getDisplayName(uri: Uri): String? {
        return appContext.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val idx = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )
                if (idx >= 0 && cursor.moveToFirst()) {
                    cursor.getString(idx)
                } else {
                    null
                }
            }
    }
}
