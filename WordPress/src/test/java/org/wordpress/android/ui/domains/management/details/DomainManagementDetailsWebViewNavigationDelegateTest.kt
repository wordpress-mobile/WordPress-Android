package org.wordpress.android.ui.domains.management.details

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class DomainManagementDetailsWebViewNavigationDelegateTest : BaseUnitTest() {
    private val navigationDelegate = DomainManagementDetailsWebViewNavigationDelegate

    @Test
    fun `when browsing in the domains path, then the web view can navigate`() {
        Assertions.assertThat(
            buildUrls(
                "/domains/manage/all/some.domain/edit/some.domain", // standard details page
                "/domains/mapping/some.domain/setup/some.domain?step=&show-errors=false&firstVisit=false" // fix errors
            )
        ).allMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `when browsing outside the domains path, then the web view cannot navigate`() {
        Assertions.assertThat(
            buildUrls(
                "/email/antonis.me/manage/some.domain", // setup email
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
        private fun buildUrls(vararg paths: String, host: String = "wordpress.com") = paths.toList().map {
            DomainManagementDetailsWebViewNavigationDelegate.Url(host, it)
        }
    }
}
