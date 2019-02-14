package org.wordpress.android.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around UrlUtils.
 *
 * UrlUtils interface is consisted of static methods, which makes the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Singleton
class UrlUtilsWrapper @Inject constructor() {
    fun extractSubDomain(domain: String): String {
        return UrlUtils.extractSubDomain(domain)
    }

    fun addUrlSchemeIfNeeded(url: String, addHttps: Boolean): String {
        return UrlUtils.addUrlSchemeIfNeeded(url, addHttps)
    }
}
