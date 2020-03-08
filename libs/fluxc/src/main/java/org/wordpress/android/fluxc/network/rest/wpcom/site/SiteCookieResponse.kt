package org.wordpress.android.fluxc.network.rest.wpcom.site

class SiteCookieResponse(
    val url: String,
    val cookies: List<SiteCookie>
)

class SiteCookie(
    val expires: String,
    val path: String,
    val domain: String,
    val name: String,
    val value: String
)
