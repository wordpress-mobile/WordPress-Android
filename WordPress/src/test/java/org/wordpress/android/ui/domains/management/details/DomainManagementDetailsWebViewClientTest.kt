package org.wordpress.android.ui.domains.management.details

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class DomainManagementDetailsWebViewClientTest : BaseUnitTest() {
    @Mock
    private lateinit var navigationDelegate: DomainManagementDetailsWebViewNavigationDelegate

    @Mock
    private lateinit var listener: DomainManagementDetailsWebViewClient.DomainManagementWebViewClientListener

    @Mock
    private lateinit var webView: WebView

    @Mock
    private lateinit var request: WebResourceRequest

    @Mock
    private lateinit var uri: Uri

    private lateinit var client: DomainManagementDetailsWebViewClient

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        client = DomainManagementDetailsWebViewClient(navigationDelegate, listener)
    }

    @Test
    fun `WHEN browsing to a not allowed domain THEN redirect to the external browser`() {
        val url = "https://some.domain"
        whenever(navigationDelegate.canNavigateTo(url)).thenReturn(false)
        client.doUpdateVisitedHistory(null, url, false)
        verify(listener, times(1)).onRedirectToExternalBrowser(url)
    }

    @Test
    fun `WHEN browsing to an allowed domain THEN do not redirect to the external browser`() {
        val url = "https://some.domain"
        whenever(navigationDelegate.canNavigateTo(url)).thenReturn(true)
        client.doUpdateVisitedHistory(null, url, false)
        verify(listener, times(0)).onRedirectToExternalBrowser(url)
    }

    @Test
    fun `WHEN browsing to the blank domain THEN do not redirect to the external browser`() {
        val url = "about:blank"
        client.doUpdateVisitedHistory(null, url, false)
        verify(listener, times(0)).onRedirectToExternalBrowser(url)
    }

    @Test
    fun `WHEN browsing to a null domain THEN do not redirect to the external browser`() {
        val url = null
        client.doUpdateVisitedHistory(null, url, false)
        verify(listener, times(0)).onRedirectToExternalBrowser(any())
    }

    @Test
    fun `WHEN preparing to browse to a not allowed domain THEN redirect to the external browser`() {
        val url = "https://some.domain"
        whenever(uri.toString()).thenReturn(url)
        whenever(request.url).thenReturn(uri)
        whenever(navigationDelegate.canNavigateTo(uri)).thenReturn(false)
        val actual = client.shouldOverrideUrlLoading(webView, request)
        verify(listener, times(1)).onRedirectToExternalBrowser(url)
        assertThat(actual).isTrue
    }

    @Test
    fun `WHEN preparing to browse to an allowed domain THEN do not redirect to the external browser`() {
        val url = "https://some.domain"
        whenever(uri.toString()).thenReturn(url)
        whenever(request.url).thenReturn(uri)
        whenever(navigationDelegate.canNavigateTo(uri)).thenReturn(true)
        val actual = client.shouldOverrideUrlLoading(webView, request)
        verify(listener, times(0)).onRedirectToExternalBrowser(url)
        assertThat(actual).isFalse
    }
}
