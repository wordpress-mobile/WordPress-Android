package org.wordpress.android.fluxc.network

import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI

/**
 * A [CookieManager] that's patched against the bug: https://issuetracker.google.com/issues/174647435
 * The logic of saving Cookies has been updated using the OpenJdk's implementation, the only changes
 * made are converting it to Kotlin and adapting for missing APIs.
 * the source can be found in: https://github.com/openjdk/jdk/blob/20db7800a657b311eeac504a2bbae4adbc209dbf/src/java.base/share/classes/java/net/CookieManager.java
 */
class PatchedCookieManager : CookieManager() {
    private val policyCallback = CookiePolicy.ACCEPT_ORIGINAL_SERVER

    @Suppress(
        "LongMethod", "ComplexMethod",
        "NestedBlockDepth", "SwallowedException",
        "MagicNumber", "ReturnCount"
    )
    @Throws(IOException::class)
    override fun put(uri: URI?, responseHeaders: Map<String?, List<String>>?) {
        // pre-condition check
        require(!(uri == null || responseHeaders == null)) { "Argument is null" }

        // if there's no default CookieStore, no need to remember any cookie
        if (cookieStore == null) return
        for (headerKey in responseHeaders.keys) {
            // RFC 2965 3.2.2, key must be 'Set-Cookie2'
            // we also accept 'Set-Cookie' here for backward compatibility
            if (headerKey == null
                || !(headerKey.equals("Set-Cookie2", ignoreCase = true)
                    || headerKey.equals("Set-Cookie", ignoreCase = true))
            ) {
                continue
            }
            for (headerValue in responseHeaders[headerKey]!!) {
                try {
                    val cookies: List<HttpCookie> = try {
                        HttpCookie.parse(headerValue)
                    } catch (e: IllegalArgumentException) {
                        // Bogus header, make an empty list and log the error
                        emptyList()
                    }
                    for (cookie in cookies) {
                        if (cookie.path == null) {
                            // If no path is specified, then by default
                            // the path is the directory of the page/doc
                            var path = uri.path
                            if (!path.endsWith("/")) {
                                val i = path.lastIndexOf('/')
                                path = if (i > 0) {
                                    path.substring(0, i + 1)
                                } else {
                                    "/"
                                }
                            }
                            cookie.path = path
                        }

                        // As per RFC 2965, section 3.3.1:
                        // Domain  Defaults to the effective request-host.  (Note that because
                        // there is no dot at the beginning of effective request-host,
                        // the default Domain can only domain-match itself.)
                        if (cookie.domain == null) {
                            var host = uri.host
                            if (host != null && !host.contains(".")) host += ".local"
                            cookie.domain = host
                        }
                        val ports = cookie.portlist
                        if (ports != null) {
                            var port = uri.port
                            if (port == -1) {
                                port = if ("https" == uri.scheme) 443 else 80
                            }
                            if (ports.isEmpty()) {
                                // Empty port list means this should be restricted
                                // to the incoming URI port
                                cookie.portlist = "" + port
                                if (shouldAcceptInternal(uri, cookie)) {
                                    cookieStore.add(uri, cookie)
                                }
                            } else {
                                // Only store cookies with a port list
                                // IF the URI port is in that list, as per
                                // RFC 2965 section 3.3.2
                                if (isInPortList(lst = ports, port = port)
                                    && shouldAcceptInternal(uri, cookie)
                                ) {
                                    cookieStore.add(uri, cookie)
                                }
                            }
                        } else {
                            if (shouldAcceptInternal(uri, cookie)) {
                                cookieStore.add(uri, cookie)
                            }
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    // invalid set-cookie header string
                    // no-op
                }
            }
        }
    }

    private fun shouldAcceptInternal(uri: URI, cookie: HttpCookie): Boolean {
        return try {
            policyCallback.shouldAccept(uri, cookie)
        } catch (ignored: Exception) { // protect against malicious callback
            false
        }
    }

    @Suppress("ReturnCount", "MagicNumber")
    private fun isInPortList(lst: String, port: Int): Boolean {
        var portsList = lst
        var i = portsList.indexOf(',')
        var value: Int
        while (i > 0) {
            try {
                value = portsList.substring(0, i).toInt(10)
                if (value == port) {
                    return true
                }
            } catch (_: NumberFormatException) {
            }
            portsList = portsList.substring(i + 1)
            i = portsList.indexOf(',')
        }
        if (portsList.isNotEmpty()) {
            try {
                value = portsList.toInt()
                if (value == port) {
                    return true
                }
            } catch (_: NumberFormatException) {
            }
        }
        return false
    }
}
