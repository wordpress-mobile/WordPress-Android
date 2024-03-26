package org.wordpress.android.ui.sitemonitor

import android.net.Uri
import android.webkit.WebResourceRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

@ExperimentalCoroutinesApi
class SiteMonitorWebViewClientTest : BaseUnitTest() {
    @Mock
    private lateinit var mockListener: SiteMonitorWebViewClient.SiteMonitorWebViewClientListener

    @Mock
    private lateinit var uri: Uri

    private lateinit var webViewClient: SiteMonitorWebViewClient

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        webViewClient = SiteMonitorWebViewClient(mockListener, SiteMonitorType.METRICS)
    }

    @Test
    fun `when onPageFinished, then should invoke on web view page loaded`() {
        webViewClient.onPageFinished(mock(), "https://example.com")

        verify(mockListener).onWebViewPageLoaded("https://example.com", SiteMonitorType.METRICS)
    }

    @Test
    fun `when onReceivedError, then should invoke on web view error received`() {
        val mockRequest: WebResourceRequest = mock()
        whenever(mockRequest.isForMainFrame).thenReturn(true)
        val url = "https://some.domain"
        whenever(uri.toString()).thenReturn(url)
        whenever(mockRequest.url).thenReturn(uri)

        webViewClient.onPageStarted(mock(), url, null)
        webViewClient.onReceivedError(
            mock(),
            mockRequest,
            mock()
        )

        verify(mockListener).onWebViewReceivedError(url, SiteMonitorType.METRICS)
    }

    @Test
    fun `when onPageFinished, then should not invoke OnReceivedError`() {
        val url = "https://some.domain"

        webViewClient.onPageFinished(mock(), url)

        verify(mockListener, never()).onWebViewReceivedError(anyString(), any())
    }
}

