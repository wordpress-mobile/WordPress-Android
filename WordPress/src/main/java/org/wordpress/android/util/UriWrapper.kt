package org.wordpress.android.util

import android.net.Uri

/**
 * This class is necessary because standard Uri doesn't work in unit tests (it's always null)
 */
data class UriWrapper(val uri: Uri) {
    constructor(uriString: String) : this(Uri.parse(uriString))
    val pathSegments: List<String> = uri.pathSegments
    val host: String? = uri.host

    override fun toString() = uri.toString()
    fun getQueryParameter(key: String): String? = uri.getQueryParameter(key)
}
