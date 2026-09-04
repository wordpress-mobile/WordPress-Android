package org.wordpress.android.fluxc.model

/**
 * The WordPress.com REST API proxy.
 *
 * Requests routed through it carry their namespace inside the path — `wp/v2/sites/<id>/…`, or
 * `wp-json/?rest_route=/sites/<domain>` for a site advertising the plain-permalink form — so a URL
 * under this host is never a usable direct-host REST root. Appending an already-namespaced path to
 * one namespaces the request twice and WordPress answers `rest_no_route`.
 *
 * [SiteModel.getWpApiRestUrl] is a direct-host root, so a proxy URL must never be stored there.
 * Code that wants the proxy addresses it through `WpComUrlResolver` instead.
 */
object WPComApiProxy {
    const val ROOT = "https://public-api.wordpress.com/"

    /** SQL `LIKE` pattern matching every [ROOT] URL, for migrations. Keep in step with [isProxyRoot]. */
    const val ROOT_LIKE_PATTERN = "$ROOT%"

    fun isProxyRoot(url: String?): Boolean = url?.startsWith(ROOT) == true
}
