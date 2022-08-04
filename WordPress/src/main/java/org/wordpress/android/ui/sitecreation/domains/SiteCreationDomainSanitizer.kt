package org.wordpress.android.ui.sitecreation.domains

import java.util.Locale
import javax.inject.Inject

class SiteCreationDomainSanitizer
@Inject constructor() {
    private val String.removeProtocol: String
        get() = this.replace("http://", "").replace("https://", "")

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
     * Sanitizes a domain suggestions query applying the algorithm from the [domains/suggestions] API endpoint.
     */
    fun sanitizeDomainQuery(query: String): String {
        return query
                .removeDisallowedCharacters
                .removeDisallowedTerms
                .trim('.', '-')
                .trim()
                .lowercase(Locale.ROOT)
    }

    /**
     * Remove or translate characters that aren't permitted in searches.
     *
     * @return The sanitized query.
     */
    private val String.removeDisallowedCharacters: String
        get() {
            val disallowedPattern = """[^\w\s.-]""".toRegex()

            return replace(disallowedPattern) { "" }
        }

    /**
     * Remove any terms that aren't permitted in searches.
     *
     * @return The sanitized query.
     */
    private val String.removeDisallowedTerms: String
        get() {
            val blacklistedStrings = arrayOf(
                    "automattic",
                    // "wordpress", // We won't mimic the API behavior here since it's producing a bad UX.
                    "wpcomstaging",
                    "paypal",
                    "bankofamerica",
                    "wellsfargo",
                    "westernunion",
                    "woocommerce",
                    "woothemes",
                    "facebook",
                    ".blogspot.",
                    "timboydaustralian.com",
                    "dssvermoegensverwaltung",
                    "alertwebmail",
                    "updaterenewbilling",
                    "securebilling",
                    "webmail-postmaster",
                    "webmail-receipt",
                    "inboxmail-dataplugs",
                    "mailerdaemon",
            )

            var sanitizedQuery = replace(".wordpress.com", "")

            blacklistedStrings.forEach {
                sanitizedQuery = sanitizedQuery.replace(it, "")
            }

            return sanitizedQuery
        }
}
