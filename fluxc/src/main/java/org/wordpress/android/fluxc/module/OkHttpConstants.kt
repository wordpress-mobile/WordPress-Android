package org.wordpress.android.fluxc.module

import okhttp3.internal.tls.OkHostnameVerifier

object OkHttpConstants {
    const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10000
    const val DEFAULT_READ_TIMEOUT_MILLIS = 10000
    const val DEFAULT_WRITE_TIMEOUT_MILLIS = 10000

    val DEFAULT_HOSTNAME_VERIFIER = OkHostnameVerifier
}
