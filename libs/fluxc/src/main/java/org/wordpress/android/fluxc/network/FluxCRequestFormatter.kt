package org.wordpress.android.fluxc.network

import com.automattic.android.tracks.crashlogging.FormattedUrl
import com.automattic.android.tracks.crashlogging.RequestFormatter
import okhttp3.Request

/**
 * Formats HTTP requests for crash logging breadcrumbs.
 * Redacts sensitive query parameters and sanitizes URLs.
 */
class FluxCRequestFormatter : RequestFormatter {
    override fun formatRequestUrl(request: Request): FormattedUrl {
        val url = request.url
        val sanitizedUrl = url.newBuilder().apply {
            // Redact sensitive query parameters
            SENSITIVE_PARAMS.forEach { param ->
                if (url.queryParameter(param) != null) {
                    removeAllQueryParameters(param)
                    addQueryParameter(param, REDACTED)
                }
            }
        }.build()

        return "${request.method} ${sanitizedUrl.encodedPath}${
            if (sanitizedUrl.encodedQuery.isNullOrEmpty()) ""
            else "?${sanitizedUrl.encodedQuery}"
        }"
    }

    companion object {
        private const val REDACTED = "[REDACTED]"
        private val SENSITIVE_PARAMS = setOf(
            "token",
            "access_token",
            "auth",
            "api_key",
            "password",
            "secret"
        )
    }
}
