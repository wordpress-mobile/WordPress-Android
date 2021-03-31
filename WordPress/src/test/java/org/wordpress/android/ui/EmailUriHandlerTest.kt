package org.wordpress.android.ui

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction
import org.wordpress.android.util.UriWrapper

class EmailUriHandlerTest : BaseUnitTest(){
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var accountStore: AccountStore
    private lateinit var handler: EmailUriHandler

    @Before
    fun setUp() {
        handler = EmailUriHandler(siteStore, accountStore)
    }

    @Test
    fun `should handle email URL`() {
        val uri = buildUri("public-api.wordpress.com", "bar")

        val shouldHandleUri = handler.shouldHandleUri(uri)

        assertThat(shouldHandleUri).isTrue()
    }

    @Test
    fun `should not handle WPcom URL`() {
        val uri = buildUri("wordpress.com", "bar")

        val shouldHandleUri = handler.shouldHandleUri(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `should not handle non-bar URL`() {
        val uri = buildUri("public-api.wordpress.com", "mbar")

        val shouldHandleUri = handler.shouldHandleUri(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `magic login URL opens the URI in the browser without redirect parameter`() {
        val uri = buildUri("public-api.wordpress.com", "bar")
        var navigateAction: NavigateAction? = null
        handler.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }
        handler.handleUri(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenInWebView(uri))
    }

    private fun buildUri(host: String, path: String): UriWrapper {
        val uri = mock<UriWrapper>()
        whenever(uri.host).thenReturn(host)
        whenever(uri.pathSegments).thenReturn(listOf(path))
        return uri
    }
}
