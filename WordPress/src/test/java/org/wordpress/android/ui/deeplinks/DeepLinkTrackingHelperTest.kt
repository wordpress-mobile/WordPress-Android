package org.wordpress.android.ui.deeplinks

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.deeplinks.DeepLinkModel.Source
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.util.UriWrapper

@RunWith(MockitoJUnitRunner::class)
class DeepLinkTrackingHelperTest {
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var deepLinkHandlers: DeepLinkHandlers
    private lateinit var deepLinkTrackingHelper: DeepLinkTrackingHelper

    @Before
    fun setUp() {
        deepLinkTrackingHelper = DeepLinkTrackingHelper(deepLinkUriUtils, deepLinkHandlers)
    }

    @Test
    fun `builds tracking data without stripped URL for OpenBrowser event`() {
        val uri = mock<UriWrapper>()
        val expectedUrl = "wordpress.com/example.com/1234"
        whenever(uri.toString()).thenReturn(expectedUrl)

        val trackingData = deepLinkTrackingHelper.buildTrackingDataFromNavigateAction(OpenInBrowser(uri), uri)

        assertThat(trackingData.source).isEqualTo(Source.EMAIL)
        assertThat(trackingData.url).isEqualTo(expectedUrl)
        assertThat(trackingData.sourceInfo).isNull()
    }


    @Test
    fun `builds tracking data from an applink URL`() {
        val uri = buildUri("reader")
        val expectedUrl = "wordpress://reader"
        whenever(deepLinkHandlers.stripUrl(uri)).thenReturn(expectedUrl)

        val trackingData = deepLinkTrackingHelper.buildTrackingDataFromNavigateAction(OpenReader, uri)

        assertThat(trackingData.source).isEqualTo(Source.BANNER)
        assertThat(trackingData.url).isEqualTo(expectedUrl)
        assertThat(trackingData.sourceInfo).isNull()
    }

    @Test
    fun `builds tracking data from redirect param of a tracking URI with a login reason`() {
        val uri = mock<UriWrapper>()
        whenever(deepLinkUriUtils.isTrackingUrl(uri)).thenReturn(true)
        val redirectUri = mock<UriWrapper>()
        val loginReason = "loginReason"
        whenever(uri.getQueryParameter("login_reason")).thenReturn(loginReason)
        whenever(deepLinkUriUtils.getRedirectUri(uri)).thenReturn(redirectUri)
        val strippedUrl = "wordpress.com/read"
        whenever(deepLinkHandlers.stripUrl(redirectUri)).thenReturn(strippedUrl)

        val trackingData = deepLinkTrackingHelper.buildTrackingDataFromNavigateAction(OpenReader, uri)

        assertThat(trackingData.source).isEqualTo(Source.EMAIL)
        assertThat(trackingData.url).isEqualTo(strippedUrl)
        assertThat(trackingData.sourceInfo).isEqualTo(loginReason)
    }

    @Test
    fun `builds tracking data from a nested param of a tracking URI without a login reason`() {
        val uri = mock<UriWrapper>()
        whenever(deepLinkUriUtils.isTrackingUrl(uri)).thenReturn(true)
        val firstRedirect = mock<UriWrapper>()
        val secondRedirect = mock<UriWrapper>()
        whenever(deepLinkUriUtils.getRedirectUri(uri)).thenReturn(firstRedirect)
        whenever(deepLinkUriUtils.getRedirectUri(firstRedirect)).thenReturn(secondRedirect)
        whenever(deepLinkUriUtils.isWpLoginUrl(firstRedirect)).thenReturn(true)
        val strippedUrl = "wordpress.com/read"
        whenever(deepLinkHandlers.stripUrl(secondRedirect)).thenReturn(strippedUrl)

        val trackingData = deepLinkTrackingHelper.buildTrackingDataFromNavigateAction(OpenReader, uri)

        assertThat(trackingData.source).isEqualTo(Source.EMAIL)
        assertThat(trackingData.url).isEqualTo(strippedUrl)
        assertThat(trackingData.sourceInfo).isNull()
    }
}
