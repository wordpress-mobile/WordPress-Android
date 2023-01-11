package org.wordpress.android.ui.deeplinks

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DEEP_LINKED
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.deeplinks.DeepLinkTrackingUtils.DeepLinkSource
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class DeepLinkTrackingUtilsTest {
    @Mock
    lateinit var deepLinkUriUtils: DeepLinkUriUtils

    @Mock
    lateinit var deepLinkHandlers: DeepLinkHandlers

    @Mock
    lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    private lateinit var deepLinkTrackingUtils: DeepLinkTrackingUtils
    private val action = "action"

    @Before
    fun setUp() {
        deepLinkTrackingUtils = DeepLinkTrackingUtils(deepLinkUriUtils, deepLinkHandlers, analyticsUtilsWrapper)
    }

    @Test
    fun `builds tracking data without stripped URL for OpenBrowser event`() {
        val host = "wordpress.com"
        val uri = buildUri(host)
        val expectedUrl = "wordpress.com/example.com/1234"
        whenever(uri.toString()).thenReturn(expectedUrl)

        deepLinkTrackingUtils.track(action, OpenInBrowser(uri), uri)

        assertTrackingData(host, DeepLinkSource.EMAIL, expectedUrl)
    }

    @Test
    fun `builds tracking data from an applink URL`() {
        val host = "reader"
        val uri = buildUri(host)
        val expectedUrl = "wordpress://reader"
        whenever(deepLinkHandlers.stripUrl(uri)).thenReturn(expectedUrl)

        deepLinkTrackingUtils.track(action, OpenReader, uri)

        assertTrackingData(host, DeepLinkSource.BANNER, expectedUrl)
    }

    @Test
    fun `builds tracking data from redirect param of a tracking URI with a login reason`() {
        val host = "public-api.wordpress.com"
        val uri = buildUri(host)
        whenever(deepLinkUriUtils.isTrackingUrl(uri)).thenReturn(true)
        val redirectUri = mock<UriWrapper>()
        val loginReason = "loginReason"
        whenever(uri.getQueryParameter("login_reason")).thenReturn(loginReason)
        whenever(deepLinkUriUtils.getRedirectUri(uri)).thenReturn(redirectUri)
        val strippedUrl = "wordpress.com/read"
        whenever(deepLinkHandlers.stripUrl(redirectUri)).thenReturn(strippedUrl)

        deepLinkTrackingUtils.track(action, OpenReader, uri)

        assertTrackingData(host, DeepLinkSource.EMAIL, strippedUrl, loginReason)
    }

    @Test
    fun `builds tracking data from a nested param of a tracking URI without a login reason`() {
        val host = "public-api.wordpress.com"
        val uri = buildUri(host)
        whenever(deepLinkUriUtils.isTrackingUrl(uri)).thenReturn(true)
        val firstRedirect = mock<UriWrapper>()
        val secondRedirect = mock<UriWrapper>()
        whenever(deepLinkUriUtils.getRedirectUri(uri)).thenReturn(firstRedirect)
        whenever(deepLinkUriUtils.getRedirectUri(firstRedirect)).thenReturn(secondRedirect)
        whenever(deepLinkUriUtils.isWpLoginUrl(firstRedirect)).thenReturn(true)
        val strippedUrl = "wordpress.com/read"
        whenever(deepLinkHandlers.stripUrl(secondRedirect)).thenReturn(strippedUrl)

        deepLinkTrackingUtils.track(action, OpenReader, uri)

        assertTrackingData(host, DeepLinkSource.EMAIL, strippedUrl)
    }

    private fun assertTrackingData(
        host: String,
        source: DeepLinkSource,
        expectedUrl: String,
        sourceInfo: String? = null
    ) {
        verify(analyticsUtilsWrapper).trackWithDeepLinkData(
            DEEP_LINKED,
            action,
            host,
            source.value,
            expectedUrl,
            sourceInfo
        )
    }
}
