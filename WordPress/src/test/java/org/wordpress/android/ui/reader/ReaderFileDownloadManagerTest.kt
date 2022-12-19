package org.wordpress.android.ui.reader

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Environment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.ui.utils.DownloadManagerWrapper

@RunWith(MockitoJUnitRunner::class)
class ReaderFileDownloadManagerTest {
    @Mock lateinit var authenticationUtils: AuthenticationUtils
    @Mock lateinit var downloadManager: DownloadManagerWrapper
    @Mock lateinit var request: DownloadManager.Request
    @Mock lateinit var query: DownloadManager.Query
    @Mock lateinit var context: Context
    @Mock lateinit var intent: Intent
    @Mock lateinit var cursor: Cursor
    private lateinit var readerFileDownloadManager: ReaderFileDownloadManager
    private lateinit var intentCaptor: KArgumentCaptor<Intent>
    @Before
    fun setUp() {
        readerFileDownloadManager = ReaderFileDownloadManager(authenticationUtils, downloadManager)
        intentCaptor = argumentCaptor()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `enqueues file for download`() {
        val url = "http://wordpress.com/file_name.pdf"
        val header = "Authentication"
        val headerValue = "token123"
        val fileName = "file_name.pdf"
        val mimeType = "application/pdf"
        whenever(authenticationUtils.getAuthHeaders(url)).thenReturn(mapOf(header to headerValue))
        whenever(downloadManager.buildRequest(url)).thenReturn(request)
        whenever(downloadManager.guessUrl(url)).thenReturn(fileName)
        whenever(downloadManager.getMimeType(url)).thenReturn(mimeType)

        readerFileDownloadManager.downloadFile(url)

        verify(downloadManager).enqueue(request)

        verify(request).addRequestHeader(header, headerValue)
        verify(request).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        verify(request).allowScanningByMediaScanner()
        verify(request).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        verify(request).setMimeType(mimeType)
        verify(request).setTitle(fileName)
    }
}
