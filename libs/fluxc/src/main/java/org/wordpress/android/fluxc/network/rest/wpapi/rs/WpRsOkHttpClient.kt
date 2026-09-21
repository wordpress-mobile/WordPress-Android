package org.wordpress.android.fluxc.network.rest.wpapi.rs

import javax.inject.Qualifier

/**
 * Qualifies the [okhttp3.OkHttpClient] that carries wordpress-rs traffic.
 *
 * Declared here so the wordpress-rs clients in this module can ask for it; the module that
 * provides it lives in the app, which depends on this one.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WpRsOkHttpClient
