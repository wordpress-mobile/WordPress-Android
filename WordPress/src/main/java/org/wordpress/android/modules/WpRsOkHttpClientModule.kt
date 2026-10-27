package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpRsOkHttpClient
import org.wordpress.android.fluxc.network.rest.wpapi.rs.applyWpRsTimeouts
import javax.inject.Named
import javax.inject.Singleton

/**
 * The OkHttp client wordpress-rs talks through.
 *
 * Every transport decision for wordpress-rs is made here, and only here: timeouts, interceptors,
 * and what is shared between clients. The API clients built on top take this one rather than
 * building their own, so a change to how wordpress-rs reaches the network is a change to this file.
 *
 * A caller needing transport of its own derives it with `newBuilder()`, which carries over the
 * connection pool, the interceptors and the timeouts. `WpApiClientProvider` does that for the
 * cookies-and-nonce client, whose cookie jar lasts one authentication attempt and so cannot be
 * shared.
 */
@InstallIn(SingletonComponent::class)
@Module
class WpRsOkHttpClientModule {
    /**
     * Shared, because an OkHttp client owns a connection pool, a dispatcher and a route cache.
     * One set of those for all of wordpress-rs means requests reuse warm connections instead of
     * every caller handshaking its own.
     *
     * The sharing stops here. Nothing above this layer is scoped, so the assumptions a long-lived
     * instance invites are confined to the transport, where there is no app state to get stale.
     */
    @Singleton
    @Provides
    @WpRsOkHttpClient
    fun provideWpRsOkHttpClient(
        @Named(OkHttpClientQualifiers.INTERCEPTORS) interceptors: Set<@JvmSuppressWildcards Interceptor>,
    ): OkHttpClient = OkHttpClient.Builder()
        .applyWpRsTimeouts()
        .apply { interceptors.forEach { addInterceptor(it) } }
        .build()
}
