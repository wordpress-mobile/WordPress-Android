package org.wordpress.android.fluxc.network.rest.wpcom.site

class PrivateAtomicCookieResponse(
    val url: String,
    val cookies: List<AtomicCookie>
)

class AtomicCookie(
    val expires: String,
    val path: String,
    val domain: String,
    val name: String,
    val value: String
)
