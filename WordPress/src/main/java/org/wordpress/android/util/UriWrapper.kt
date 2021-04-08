package org.wordpress.android.util

import android.net.Uri
import android.net.Uri.Builder

/**
 * This class is necessary because standard Uri doesn't work in unit tests (it's always null)
 */
data class UriWrapper(val uri: Uri) {
    constructor(uriString: String) : this(Uri.parse(uriString))

    val pathSegments: List<String> = uri.pathSegments
    val host: String? = uri.host

    override fun toString() = uri.toString()
    fun getQueryParameter(key: String): String? = uri.getQueryParameter(key)
    fun copy(path: String): UriWrapper {
        val newUri = Builder()
                .scheme(uri.scheme)
                .path(path)
                .query(uri.query)
                .fragment(uri.fragment)
                .authority(uri.authority)
                .build()
        return this.copy(uri = newUri)
    }
}
