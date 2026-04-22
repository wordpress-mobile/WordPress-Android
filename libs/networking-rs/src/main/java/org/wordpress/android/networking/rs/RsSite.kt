package org.wordpress.android.networking.rs

/**
 * Site descriptor used by the RS networking layer. Carries only what
 * the wordpress-rs client needs to route and authenticate a request,
 * with no fluxc or ORM coupling.
 *
 * Fluxc's `SiteModel.toRsSite()` extension is the canonical conversion
 * point — call sites that already hold a `SiteModel` should use that
 * rather than constructing an `RsSite` directly.
 */
data class RsSite(
    /** Local database id — used to key cached self-hosted clients. */
    val localId: Int,
    /** WP.com remote site id (0 when unknown). */
    val siteId: Long,
    /** Origin site URL — e.g. "https://example.com". */
    val url: String,
    /** Pre-resolved `/wp-json/` URL when the site has stored one. */
    val wpApiRestUrl: String?,
    val isWPCom: Boolean,
    val isWPComAtomic: Boolean,
    val isJetpackConnected: Boolean,
    /**
     * True when the site's REST API reaches us via WP.com
     * (`SiteModel.ORIGIN_WPCOM_REST`). XMLRPC-origin Jetpack sites have
     * this false even when they are Jetpack-connected.
     */
    val isUsingWpComRestApi: Boolean,
    /** Application-password username (`""` when not provisioned). */
    val apiRestUsernamePlain: String,
    /** Application-password secret (`""` when not provisioned). */
    val apiRestPasswordPlain: String,
    /** WP admin username — used by the cookies-nonce auth flow. */
    val username: String,
    /** WP admin password — used by the cookies-nonce auth flow. */
    val password: String,
) {
    val isWPComSimpleSite: Boolean
        get() = isWPCom && !isWPComAtomic

    val hasApplicationPassword: Boolean
        get() = apiRestUsernamePlain.isNotEmpty() &&
                apiRestPasswordPlain.isNotEmpty()

    /**
     * Resolves the REST API root URL for this site — either the
     * pre-resolved `/wp-json/` URL stored on the site, or the
     * conventional `${url}/wp-json` fallback.
     */
    fun buildRestApiUrl(): String =
        wpApiRestUrl?.takeIf { it.isNotEmpty() } ?: "$url/wp-json"
}

/**
 * Decides whether a site's API requests go through the
 * `public-api.wordpress.com` proxy or directly to the site.
 *
 * See `docs/wpcom-routing.md` for the full spec.
 */
fun RsSite.shouldUseWpComProxy(): Boolean = when {
    isWPComSimpleSite -> true
    (isWPComAtomic || isJetpackConnected) && !hasApplicationPassword -> true
    else -> false
}
