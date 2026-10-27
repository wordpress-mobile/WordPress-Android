package org.wordpress.android.networking.restapi

import org.wordpress.android.fluxc.store.AccountStore
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpDynamicAuthenticationProvider
import javax.inject.Inject

/**
 * Reads the WordPress.com bearer token from [AccountStore] as each request goes out, rather than
 * when the client was built. A client can then outlive a sign out and the sign in that follows it
 * and still authenticate as whoever is signed in now, so nothing has to be rebuilt or invalidated
 * when the token changes.
 *
 * With no token the request is sent unauthenticated: some WordPress.com endpoints answer without
 * one, and the rest reject it.
 *
 * [AccountStore.getAccessToken] is typed nullable but reads `""` when signed out, and is only null
 * between an in-process sign out and the next launch, so both mean no token.
 */
class WpComAuthenticationProvider @Inject constructor(
    private val accountStore: AccountStore,
) : WpDynamicAuthenticationProvider {
    override fun auth(): WpAuthentication =
        accountStore.accessToken
            ?.takeIf { it.isNotEmpty() }
            ?.let { WpAuthentication.Bearer(token = it) }
            ?: WpAuthentication.None

    /**
     * The app cannot renew a WordPress.com token on its own — a rejected one is replaced by signing
     * in again — so a retry would be sent with the token that was just rejected.
     */
    override suspend fun refresh(): Boolean = false
}
