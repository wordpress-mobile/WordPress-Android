package org.wordpress.android.fluxc.module

/**
 * Constants for @Named qualifiers used with OkHttpClient and RequestQueue dependency injection.
 *
 * These constants should be used instead of hardcoded strings to prevent typos
 * and enable easier refactoring.
 */
object OkHttpClientQualifiers {
    const val REGULAR = "regular"
    const val CUSTOM_SSL = "custom-ssl"
    const val NO_REDIRECTS = "no-redirects"
    const val NO_COOKIES = "no-cookies"
    const val CUSTOM_SSL_CUSTOM_REDIRECTS = "custom-ssl-custom-redirects"
    const val INTERCEPTORS = "interceptors"
    const val NETWORK_INTERCEPTORS = "network-interceptors"
}
