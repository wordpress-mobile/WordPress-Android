package org.wordpress.android.networking.restapi

import org.wordpress.android.fluxc.store.AccountStore
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpDynamicAuthenticationProvider

/**
 * Creates a [WpAuthenticationProvider] that reads the WordPress.com
 * OAuth bearer token from [AccountStore] on every request. This ensures
 * cached API clients automatically pick up refreshed tokens without
 * needing to be recreated.
 */
fun createWpComAuthProvider(accountStore: AccountStore): WpAuthenticationProvider =
    WpAuthenticationProvider.dynamic(object : WpDynamicAuthenticationProvider {
        override fun auth() = WpAuthentication.Bearer(
            token = requireNotNull(accountStore.accessToken) {
                "WP.com access token is required"
            }
        )
        override suspend fun refresh() = accountStore.accessToken != null
    })
