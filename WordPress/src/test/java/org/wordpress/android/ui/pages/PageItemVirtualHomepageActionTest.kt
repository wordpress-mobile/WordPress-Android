package org.wordpress.android.ui.pages

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

class PageItemVirtualHomepageActionTest {
    @Test
    fun `when OpenSiteEditor getUrl is invoked then return correct url for site`() {
        val testSites = listOf(
            "https://mysite.wordpress.com/wp-admin/",
            "https://mysite.self.host/wp-admin/"
        )

        testSites.forEach { testSite ->
            val site = SiteModel().apply { adminUrl = testSite }
            val action = PageItem.VirtualHomepage.Action.OpenSiteEditor()

            val url = action.getUrl(site)

            assertThat(url).isEqualTo("${testSite}site-editor.php?canvas=edit")
        }
    }
}
