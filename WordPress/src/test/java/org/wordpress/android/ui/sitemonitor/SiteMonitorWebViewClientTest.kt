package org.wordpress.android.ui.sitemonitor

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import android.webkit.WebResourceError
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
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
        webViewClient.onPageFinished(mock(WebView::class.java), "https://example.com")

        verify(mockListener).onWebViewPageLoaded("https://example.com", SiteMonitorType.METRICS)
    }

    @Test
    fun `when onReceivedError, then should invoke on web view error received`() {
        val mockRequest = mock(WebResourceRequest::class.java)
        whenever(mockRequest.isForMainFrame).thenReturn(true)
        val url = "https://some.domain"
        whenever(uri.toString()).thenReturn(url)
        whenever(mockRequest.url).thenReturn(uri)

        webViewClient.onPageStarted(mock(WebView::class.java), url, null)
        webViewClient.onReceivedError(
            mock(WebView::class.java),
            mockRequest,
            mock(WebResourceError::class.java)
        )

        verify(mockListener).onWebViewReceivedError(url, SiteMonitorType.METRICS)
    }

    @Test
    fun `when onPageFinished, then should not invoke OnReceivedError`() {
        val url = "https://some.domain"

        webViewClient.onPageFinished(mock(WebView::class.java), url)

        verify(mockListener, never()).onWebViewReceivedError(anyString(), any())
    }
}

