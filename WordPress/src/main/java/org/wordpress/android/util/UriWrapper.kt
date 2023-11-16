package org.wordpress.android.util

import android.net.Uri

/**
 * This class is necessary because standard Uri doesn't work in unit tests (it's always null)
 */
data class UriWrapper(val uri: Uri) {
    constructor(uriString: String) : this(Uri.parse(uriString))

    val lastPathSegment: String? = uri.lastPathSegment
    val pathSegments: List<String> = uri.pathSegments
    val host: String? = uri.host
    val fragment: String? = uri.fragment

    override fun toString() = uri.toString()
    fun getQueryParameter(key: String): String? = uri.getQueryParameter(key)
    fun copy(path: String): UriWrapper {
        val newUri = uri.buildUpon().path(path).build()
        return this.copy(uri = newUri)
    }
}

/**
 * Note, java.net.URLEncoder is not currently a suitable alternative to using Uri.encode in the main codebase.
 * This is because java.net.URLEncoder requires API 33 or later (we support down to API 24 at the time of writing).
 * However, java.net.URLEncoder can be used in unit tests to avoid fully mocking the encode function.
 * e.g. whenever(uriWrapper.encode(value)).thenReturn(URLEncoder.encode(value, StandardCharsets.UTF_8))
 */
class UriEncoder {
    fun encode(input: String): String {
        return Uri.encode(input)
    }
}
