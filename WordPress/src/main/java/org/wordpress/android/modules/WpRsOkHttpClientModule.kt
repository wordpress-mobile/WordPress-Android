package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.rest.wpapi.rs.applyWpRsTimeouts
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

/** The [OkHttpClient] that carries wordpress-rs traffic. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WpRsOkHttpClient

/**
 * The OkHttp clients wordpress-rs talks through.
 *
 * Every transport decision for wordpress-rs is made here, and only here: timeouts, interceptors,
 * and what is shared between clients. The API clients built on top take one of these rather than
 * building their own, so a change to how wordpress-rs reaches the network is a change to this file.
 *
 * A caller that needs different transport gets another binding in this module, derived from
 * [provideWpRsOkHttpClient] with `newBuilder()` so it keeps the shared connection pool.
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
