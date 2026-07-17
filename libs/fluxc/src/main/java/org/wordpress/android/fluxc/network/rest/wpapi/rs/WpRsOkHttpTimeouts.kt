package org.wordpress.android.fluxc.network.rest.wpapi.rs

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_SECONDS = 30L
private const val READ_TIMEOUT_SECONDS = 60L
private const val WRITE_TIMEOUT_SECONDS = 60L

/**
 * Applies the shared OkHttp timeouts used by wordpress-rs clients that supply their own
 * [OkHttpClient] via `WpHttpClient.CustomOkHttpClient`. Those clients bypass the library's
 * `DefaultHttpClient`, so without this they fall back to OkHttp's short 10s per-operation
 * defaults, which time out prematurely on slow sites or large responses.
 */
fun OkHttpClient.Builder.applyWpRsTimeouts(): OkHttpClient.Builder =
    connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
