package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.ui.deeplinks.buildUri
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class QRCodeMediaLinkHandlerTest {
    @Mock
    lateinit var deepLinkUriUtils: DeepLinkUriUtils

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var site: SiteModel
    private lateinit var qrCodeMediaLinkHandler: QRCodeMediaLinkHandler

    @Before
    fun setUp() {
        qrCodeMediaLinkHandler = QRCodeMediaLinkHandler(deepLinkUriUtils, analyticsTrackerWrapper)
    }

    // https://apps.wordpress.com/get/?campaign=qr-code-media#/media/225903215
    @Test
    fun `given proper media url, when deep linked, then handles URI`() {
        val mediaUri = buildUri(
            host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "qr-code-media"),
            path = arrayOf("get")
        )

        val isMediaQrCodeUri = qrCodeMediaLinkHandler.shouldHandleUrl(mediaUri)

        assertThat(isMediaQrCodeUri).isTrue()
    }

    @Test
    fun `given improper media host, when deep linked, then URI is not handled`() {
        val mediaUri = buildUri(host = "apps.wordpress.org", "get")

        val isPagesUri = qrCodeMediaLinkHandler.shouldHandleUrl(mediaUri)

        assertThat(isPagesUri).isFalse()
    }

    @Test
    fun `given improper media query params, when deep linked, then handles URI`() {
        val mediaUri = buildUri(host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "qr-code-no-good"),
            path = arrayOf("get"), )

        val isMediaQrCodeUri = qrCodeMediaLinkHandler.shouldHandleUrl(mediaUri)

        assertThat(isMediaQrCodeUri).isFalse()
    }

    @Test
    fun `given improper media path, when deep linked, then URI is not handled`() {
        val mediaUri = buildUri(host = "apps.wordpress.com", "invalid")

        val isMediaQrCodeUri = qrCodeMediaLinkHandler.shouldHandleUrl(mediaUri)

        assertThat(isMediaQrCodeUri).isFalse()
    }

    @Test
    fun `given unrecognized siteId, when deep linked, then opens my site view with message`() {
        val mediaUri = buildUri(host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "qr-code-media"),
            path = arrayOf("get"), )

        whenever(mediaUri.fragment).thenReturn(null)

        val navigateAction = qrCodeMediaLinkHandler.buildNavigateAction(mediaUri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenMySiteWithMessage(R.string.qrcode_media_deeplink_error))
    }

    @Test
    fun `given recognized siteId, when deep linked, then opens media launcher view`() {
        val siteId = "227148183"
        val mediaUri = buildUri()
        whenever(mediaUri.fragment).thenReturn("/media/$siteId")

        whenever(deepLinkUriUtils.blogIdToSite(siteId)).thenReturn(site)

        val navigateAction = qrCodeMediaLinkHandler.buildNavigateAction(mediaUri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenMediaPickerForSite(site))
    }

    @Test
    fun `given unrecognized siteId, when deep linked, then event is tracked`() {
        val mediaUri = buildUri(host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "qr-code-media"),
            path = arrayOf("get"), )

        whenever(mediaUri.fragment).thenReturn(null)

        qrCodeMediaLinkHandler.buildNavigateAction(mediaUri)

        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.DEEP_LINK_FAILED,
            mapOf("error" to "invalid_site_id",
                "campaign" to "qr_code_media"))
    }
}
