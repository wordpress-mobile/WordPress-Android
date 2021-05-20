package org.wordpress.android.ui.deeplinks

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction

@RunWith(MockitoJUnitRunner::class)
class PagesLinkHandlerTest {
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var site: SiteModel
    private lateinit var pagesLinkHandler: PagesLinkHandler

    @Before
    fun setUp() {
        pagesLinkHandler = PagesLinkHandler(deepLinkUriUtils)
    }

    @Test
    fun `handles pages URI`() {
        val pagesUri = buildUri(host = "wordpress.com", path1 = "pages")

        val isPagesUri = pagesLinkHandler.isPagesUrl(pagesUri)

        assertThat(isPagesUri).isTrue()
    }

    @Test
    fun `does not handle pages URI with different host`() {
        val pagesUri = buildUri(host = "wordpress.org", path1 = "pages")

        val isPagesUri = pagesLinkHandler.isPagesUrl(pagesUri)

        assertThat(isPagesUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val pagesUri = buildUri(host = "wordpress.com", path1 = "post")

        val isPagesUri = pagesLinkHandler.isPagesUrl(pagesUri)

        assertThat(isPagesUri).isFalse()
    }

    @Test
    fun `opens pages screen from empty URL`() {
        val uri = buildUri(path1 = "pages")

        val navigateAction = pagesLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenPages)
    }

    @Test
    fun `opens pages screen for a site when URL ends with site URL`() {
        val siteUrl = "example.com"
        val uri = buildUri(path1 = "pages", path2 = siteUrl)
        whenever(uri.lastPathSegment).thenReturn(siteUrl)
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val navigateAction = pagesLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenPagesForSite(site))
    }
}
