package org.wordpress.android.fluxc.utils.extensions

import java.net.URLEncoder

/**
 * Encodes delimiting characters as per
 * [RFC 3986](https://datatracker.ietf.org/doc/html/rfc3986#section-2.2)
 */
fun String.encodeRfc3986Delimiters(): String {
    val rfc3986Delimiters = "!'()*"

    return replace("[$rfc3986Delimiters]".toRegex()) {
        URLEncoder.encode(it.value, "UTF-8")
    }
}

fun String.slashJoin(end: String) = "${this.trimEnd('/')}/${end.trimStart('/')}"