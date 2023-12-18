package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.buildUri

@RunWith(MockitoJUnitRunner::class)
class QRCodeAuthLinkHandlerTest {
    @Mock
    lateinit var site: SiteModel
    private lateinit var qrCodeAuthLinkHandler: QRCodeAuthLinkHandler

    @Before
    fun setUp() {
        qrCodeAuthLinkHandler = QRCodeAuthLinkHandler()
    }

    // https://apps.wordpress.com/get/?campaign=login-qr-code#qr-code-login?token=XXXX&data=XXXXX
    @Test
    fun `given proper auth url, when deep linked, then handles URI`() {
        val authUri = buildUri(
            host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "login-qr-code", "token" to "XXX", "data" to "XXX"),
            fragment = "qr-code-login",
            "get",
        )

        val isAuthUri = qrCodeAuthLinkHandler.shouldHandleUrl(authUri)

        assertThat(isAuthUri).isTrue()
    }

    @Test
    fun `given improper auth host, when deep linked, then URI is not handled`() {
        val authUri = buildUri(host = "apps.wordpress.org", "get")

        val isAuthUri = qrCodeAuthLinkHandler.shouldHandleUrl(authUri)

        assertThat(isAuthUri).isFalse()
    }

    @Test
    fun `given improper media query params, when deep linked, then handles URI`() {
        val authUri = buildUri(host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "login-qr-no-good", "token" to "XXX", "data" to "XXX"),
            "get", )

        val isAuthUri = qrCodeAuthLinkHandler.shouldHandleUrl(authUri)

        assertThat(isAuthUri).isFalse()
    }

    @Test
    fun `given improper auth path, when deep linked, then URI is not handled`() {
        val authUri = buildUri(host = "apps.wordpress.com", "invalid")

        val isMediaQrCodeUri = qrCodeAuthLinkHandler.shouldHandleUrl(authUri)

        assertThat(isMediaQrCodeUri).isFalse()
    }

    @Test
    fun `given proper auth url, when deep linked, then opens qr code auth flow`() {
        val authUri = buildUri(host = "apps.wordpress.com",
            queryParams = mapOf("campaign" to "login-qr-no-good", "token" to "token", "data" to "data"),
            fragment = "qr-code-login",
            "get", )

        val navigateAction = qrCodeAuthLinkHandler.buildNavigateAction(authUri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenQRCodeAuthFlow(authUri.toString()))
    }
}
