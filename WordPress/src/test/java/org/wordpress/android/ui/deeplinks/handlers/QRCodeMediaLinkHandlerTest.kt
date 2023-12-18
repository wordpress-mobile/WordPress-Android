package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.ui.deeplinks.buildUri

@RunWith(MockitoJUnitRunner::class)
class QRCodeMediaLinkHandlerTest {
    @Mock
    lateinit var deepLinkUriUtils: DeepLinkUriUtils

    @Mock
    lateinit var site: SiteModel
    private lateinit var qrCodeMediaLinkHandler: QRCodeMediaLinkHandler

    @Before
    fun setUp() {
        qrCodeMediaLinkHandler = QRCodeMediaLinkHandler(deepLinkUriUtils)
    }

    // https://apps.wordpress.com/get/?campaign=qr-code-media&data=post_id:6,site_id:227148183
    @Test
    fun `given proper media url, when deep linked, then handles URI`() {
        val mediaUri = buildUri(
            host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "qr-code-media", "data" to "post_id:6,site_id:227148183"),
            "get",
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
            queryParams = mapOf("campaign" to "qr-code-no-good", "data" to "post_id:6,site_id:227148183"),
            "get", )

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
    fun `given unrecognized siteId, when deep linked, then opens my site view`() {
        val mediaUri = buildUri(host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "qr-code-media", "data" to "post_id:6,site_id:227148183"),
            "get", )

        whenever(mediaUri.getQueryParameter("data")).thenReturn(null)

        val navigateAction = qrCodeMediaLinkHandler.buildNavigateAction(mediaUri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenMySite)
    }

    @Test
    fun `given recognized siteId, when deep linked, then opens media view`() {
        val siteId = "227148183"
        val data = "post_id:6,site_id:227148183"
        val mediaUri = buildUri(host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "qr-code-media", "data" to "post_id:6,site_id:227148183"),
            "get", )

        whenever(mediaUri.getQueryParameter("data")).thenReturn(data)
        whenever(deepLinkUriUtils.blogIdToSite(siteId)).thenReturn(site)

        val navigateAction = qrCodeMediaLinkHandler.buildNavigateAction(mediaUri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenMediaForSite(site))
    }
}
