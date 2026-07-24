package org.wordpress.android.ui.accounts.login

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.OAuth2Endpoints
import uniffi.wp_api.ParsedUrl

/**
 * Uses Robolectric because [org.wordpress.android.util.WPUrlUtils.isWordPressCom] relies on
 * android.net.Uri to extract the host.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class DiscoverSuccessWrapperTest {
    private val wrapper = ApplicationPasswordLoginHelper.DiscoverSuccessWrapper()

    @Test
    fun `given OAuth2 with a wordpress_com endpoint, when isWpComSite, then returns true`() {
        val result = oAuth2Success("https://public-api.wordpress.com/oauth2/authorize")

        assertTrue(wrapper.isWpComSite(result))
    }

    @Test
    fun `given OAuth2 with a non-wordpress_com endpoint, when isWpComSite, then returns false`() {
        // A self-hosted site exposing OAuth2 via a plugin must not be treated as WordPress.com.
        val result = oAuth2Success("https://malicious.example.com/oauth2/authorize")

        assertFalse(wrapper.isWpComSite(result))
    }

    @Test
    fun `given ApplicationPasswords mechanism, when isWpComSite, then returns false`() {
        val authentication = DiscoveredAuthenticationMechanism.ApplicationPasswords(mock<ParsedUrl>())
        val result = ApiDiscoveryResult.Success(
            AutoDiscoveryAttemptSuccess(mock(), mock(), mock(), authentication)
        )

        assertFalse(wrapper.isWpComSite(result))
    }

    private fun oAuth2Success(authorizationUrl: String): ApiDiscoveryResult.Success {
        val authentication = DiscoveredAuthenticationMechanism.OAuth2(
            OAuth2Endpoints(authorizationUrl = authorizationUrl, tokenUrl = authorizationUrl)
        )
        return ApiDiscoveryResult.Success(
            AutoDiscoveryAttemptSuccess(mock(), mock(), mock(), authentication)
        )
    }
}
