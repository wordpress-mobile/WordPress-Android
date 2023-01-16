package org.wordpress.android.ui.reader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.ui.utils.DownloadManagerWrapper
import javax.inject.Inject

class ReaderFileDownloadManager
@Inject constructor(
    private val authenticationUtils: AuthenticationUtils,
    private val downloadManager: DownloadManagerWrapper
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
            val downloadId = intent.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID, 0
            )
            openDownloadedAttachment(context, downloadId)
        }
    }

    @Suppress("DEPRECATION")
    fun downloadFile(fileUrl: String) {
        val request = downloadManager.buildRequest(fileUrl)

        for (entry in authenticationUtils.getAuthHeaders(fileUrl).entries) {
            request.addRequestHeader(entry.key, entry.value)
        }

        val fileName = downloadManager.guessUrl(fileUrl)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setMimeType(downloadManager.getMimeType(fileUrl))
        request.setTitle(fileName)
        request.allowScanningByMediaScanner()

        downloadManager.enqueue(request)
    }

    private fun openDownloadedAttachment(context: Context, downloadId: Long) {
        val query = downloadManager.buildQuery()
        query.setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val downloadStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloadLocalUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val downloadMimeType = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL && downloadLocalUri != null) {
                downloadManager.openDownloadedAttachment(context, downloadLocalUri, downloadMimeType)
            }
        }
        cursor.close()
    }
}
