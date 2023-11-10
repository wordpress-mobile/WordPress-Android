package org.wordpress.android.ui.domains.management.details

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.utils.AbstractAllowedUrlsWebViewNavigationDelegate.Url

@ExperimentalCoroutinesApi
class DomainManagementDetailsWebViewNavigationDelegateTest : BaseUnitTest() {
    private val navigationDelegate = DomainManagementDetailsWebViewNavigationDelegate("some.domain")

    @Test
    fun `when browsing in the domain details edit path, then the web view can navigate`() {
        Assertions.assertThat(
            buildUrls(
                "/domains/manage/all/some.domain/edit/some.slug", // standard details page
            )
        ).allMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `when browsing outside the domain details edit path, then the web view cannot navigate`() {
        Assertions.assertThat(
            buildUrls(
                "/domains/manage/all/some.domain/dns/some.slug", // dns page
                "/domains/mapping/some.domain/setup/some.domain?step=&show-errors=false&firstVisit=false",// some errors
                "/email/some.domain/manage/some.domain", // setup email
                "/support/domains/https-ssl/" // support
            )
        ).noneMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `when browsing outside in another host, then the web view cannot navigate`() {
        Assertions.assertThat(
            buildUrls(
                "/domains/manage/all/some.domain/edit/some.domain",
                host = "google.com"
            )
        ).noneMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    companion object {
        private fun buildUrls(vararg paths: String, host: String = "wordpress.com") =
            paths.toList().map { Url(host, it) }
    }
}
