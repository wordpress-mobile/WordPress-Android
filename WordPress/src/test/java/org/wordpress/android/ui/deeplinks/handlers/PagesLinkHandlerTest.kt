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
class PagesLinkHandlerTest {
    @Mock
    lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock
    lateinit var site: SiteModel
    private lateinit var pagesLinkHandler: PagesLinkHandler

    @Before
    fun setUp() {
        pagesLinkHandler = PagesLinkHandler(deepLinkUriUtils)
    }

    @Test
    fun `handles pages URI`() {
        val pagesUri = buildUri(host = "wordpress.com", "pages")

        val isPagesUri = pagesLinkHandler.shouldHandleUrl(pagesUri)

        assertThat(isPagesUri).isTrue()
    }

    @Test
    fun `does not handle pages URI with different host`() {
        val pagesUri = buildUri(host = "wordpress.org", "pages")

        val isPagesUri = pagesLinkHandler.shouldHandleUrl(pagesUri)

        assertThat(isPagesUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val pagesUri = buildUri(host = "wordpress.com", "post")

        val isPagesUri = pagesLinkHandler.shouldHandleUrl(pagesUri)

        assertThat(isPagesUri).isFalse()
    }

    @Test
    fun `opens pages screen from empty URL`() {
        val uri = buildUri(host = null, "pages")

        val navigateAction = pagesLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenPages)
    }

    @Test
    fun `opens pages screen for a site when URL ends with site URL`() {
        val siteUrl = "example.com"
        val uri = buildUri(host = null, "pages", siteUrl)
        whenever(uri.lastPathSegment).thenReturn(siteUrl)
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val navigateAction = pagesLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenPagesForSite(site))
    }
}
