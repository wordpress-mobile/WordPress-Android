package org.wordpress.android.networking.restapi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import uniffi.wp_api.WpAuthentication

@ExperimentalCoroutinesApi
class WpComAuthenticationProviderTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    private lateinit var authenticationProvider: WpComAuthenticationProvider

    @Before
    fun setUp() {
        authenticationProvider = WpComAuthenticationProvider(accountStore)
    }

    @Test
    fun `given a token, when auth, then it is sent as a bearer token`() {
        whenever(accountStore.accessToken).thenReturn("a-token")

        assertThat(authenticationProvider.auth())
            .isEqualTo(WpAuthentication.Bearer("a-token"))
    }

    /**
     * `AccountStore.getAccessToken` reads `""` rather than null when signed out, so an empty token
     * has to be treated as no token. Sent as a bearer token it would be an `Authorization` header
     * with nothing after `Bearer`.
     */
    @Test
    fun `given an empty token, when auth, then no authentication is sent`() {
        whenever(accountStore.accessToken).thenReturn("")

        assertThat(authenticationProvider.auth()).isEqualTo(WpAuthentication.None)
    }

    @Test
    fun `given no token, when auth, then no authentication is sent`() {
        whenever(accountStore.accessToken).thenReturn(null)

        assertThat(authenticationProvider.auth()).isEqualTo(WpAuthentication.None)
    }

    @Test
    fun `given the token changes, when auth, then the current one is sent`() {
        whenever(accountStore.accessToken).thenReturn("first-token", "second-token")

        assertThat(authenticationProvider.auth())
            .isEqualTo(WpAuthentication.Bearer("first-token"))
        assertThat(authenticationProvider.auth())
            .isEqualTo(WpAuthentication.Bearer("second-token"))
    }

    /**
     * The app has no way to renew a WordPress.com token, so reporting a successful refresh would
     * have the library retry with the token that was just rejected.
     */
    @Test
    fun `when refresh, then it reports that nothing was refreshed`() = test {
        assertThat(authenticationProvider.refresh()).isFalse()
    }
}
