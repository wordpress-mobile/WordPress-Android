package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewNavigationDelegate.Url

@ExperimentalCoroutinesApi
class DomainRegistrationCheckoutWebViewNavigationDelegateTest : BaseUnitTest() {
    private val navigationDelegate = DomainRegistrationCheckoutWebViewNavigationDelegate

    @Test
    fun `checkout web view can navigate to checkout paths`() {
        assertThat(
            buildUrls(
                "/checkout/",
                "/checkout/dummywpcomsite.wordpress.com",
                "/checkout/thank-you/"
            )
        ).allMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `checkout web view can navigate to TOS paths`() {
        assertThat(
            buildUrls(
                "/tos/",
                "/en/tos/",
                "/pt-br/tos/"
            )
        ).allMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `checkout web view can navigate to registration agreement paths`() {
        assertThat(
            buildUrls(
                "/automattic-domain-name-registration-agreement/"
            )
        ).allMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `checkout web view can navigate to support paths`() {
        assertThat(
            buildUrls(
                "/support/",
                "/es/support/",
                "/pt-br/support/",
                "/support/payment/#using-a-payment-method-for-all-subscriptions",
                "/es/support/payment/#using-a-payment-method-for-all-subscriptions",
                "/pt-br/support/payment/#using-a-payment-method-for-all-subscriptions"
            )
        ).allMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `checkout web view cannot navigate to unallowed paths`() {
        assertThat(
            buildUrls(
                "/blog/",
                "/plans/",
                "/themes/",
                "/invalid/support/"
            )
        ).noneMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `checkout web view can navigate to host subdomain`() {
        assertThat(navigationDelegate.canNavigateTo(Url("subdomain.wordpress.com", "/checkout"))).isTrue
    }

    companion object {
        private fun buildUrls(vararg paths: String) = paths.toList().map { Url("wordpress.com", it) }
    }
}
