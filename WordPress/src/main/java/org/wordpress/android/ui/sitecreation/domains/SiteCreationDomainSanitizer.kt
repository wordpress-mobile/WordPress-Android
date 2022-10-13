package org.wordpress.android.ui.sitecreation.domains

import java.util.Locale
import javax.inject.Inject

class SiteCreationDomainSanitizer
@Inject constructor() {
    private val String.removeProtocol: String
        get() = this.replace("http://", "").replace("https://", "")

    private val String.removeNonAlphanumeric: String
        get() = this.replace("[^a-zA-Z0-9]".toRegex(), "")

    private val String.firstPeriodIndex: Int?
        get() = this.indexOf(".").let { return if (it > 0) it else null }

    private val String.firstPart: String
        get() = this.firstPeriodIndex?.let { return substring(0, it) } ?: this

    private val String.lastPart: String
        get() = this.firstPeriodIndex?.let { return substring(it) } ?: this

    /**
     * Returns the first part of the domain
     */
    fun getName(url: String) = url.firstPart.removeProtocol

    /**
     * Returns the last part of the domain (after the first .)
     */
    fun getDomain(url: String) = url.lastPart

    /**
     * Sanitizes a query by taking the value before it's first period if it's present,
     * removes it's scheme and finally, removes any characters that aren't alphanumeric.
     */
    fun sanitizeDomainQuery(query: String) =
            query.firstPart.removeProtocol.removeNonAlphanumeric.lowercase(Locale.ROOT)
}
