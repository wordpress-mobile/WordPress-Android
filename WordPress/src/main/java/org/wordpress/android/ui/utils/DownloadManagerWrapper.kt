package org.wordpress.android.ui.utils

import android.app.DownloadManager
import android.app.DownloadManager.Query
import android.app.DownloadManager.Request
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import org.wordpress.android.BuildConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManagerWrapper
@Inject constructor(private val context: Context) {
    fun enqueue(request: Request): Long = downloadManager().enqueue(request)

    fun buildRequest(fileUrl: String) = Request(Uri.parse(fileUrl))

    fun query(query: Query): Cursor = downloadManager().query(query)

    fun buildQuery() = Query()

    fun guessUrl(fileUrl: String): String = URLUtil.guessUrl(fileUrl)

    fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            val mime = MimeTypeMap.getSingleton()
            type = mime.getMimeTypeFromExtension(extension)
        }
        return type
    }

    private fun toPublicUri(fileUrl: String): Uri {
        val fileUri = Uri.parse(fileUrl)
        return if (ContentResolver.SCHEME_FILE == fileUri.scheme) {
            fileUri.path?.let { path ->
                val file = File(path)
                FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    file
                )
            } ?: fileUri
        } else {
            fileUri
        }
    }

    fun openDownloadedAttachment(
        context: Context,
        fileUrl: String,
        attachmentMimeType: String
    ) {
        val attachmentUri = toPublicUri(fileUrl)

        val openAttachmentIntent = Intent(Intent.ACTION_VIEW)
        openAttachmentIntent.setDataAndType(attachmentUri, attachmentMimeType)
        openAttachmentIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(openAttachmentIntent)
        } catch (e: ActivityNotFoundException) {
            AppLog.e(T.READER, "No browser found on the device: ${e.message}")
        }
    }

    private fun downloadManager() =
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
}
