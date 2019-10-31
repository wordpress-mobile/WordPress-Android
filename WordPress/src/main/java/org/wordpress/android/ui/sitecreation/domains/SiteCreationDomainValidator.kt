package org.wordpress.android.ui.sitecreation.domains

import org.wordpress.android.util.UrlUtilsWrapper
import java.net.URL
import javax.inject.Inject

class SiteCreationDomainValidator
@Inject constructor(
    private val urlUtilsWrapper: UrlUtilsWrapper,
    private val suffixDatabase: PublicSuffixDatabaseWrapper
) {
    /**
     * Checks to see if the supplied query is a valid domain. If not then it could be a word, so a WP.com
     * sub domain is created with it and then that's checked. If none of those are valid then we assume the supplied
     * query isn't a valid domain for site creation.
     */
    fun validateDomain(query: String): ValidationResult {
        var isValidUrl: Boolean

        var fullUrl = urlUtilsWrapper.addUrlSchemeIfNeeded(query, false)

        isValidUrl = urlUtilsWrapper.isValidUrlAndHostNotNull(fullUrl) && suffixDatabase.getEffectiveTldPlusOne(
                fullUrl
        ) != null

        return if (!isValidUrl) {
            fullUrl = fullUrl.plus(".wordpress.com")
            isValidUrl = urlUtilsWrapper.isValidUrlAndHostNotNull(fullUrl)
            if (isValidUrl) {
                ValidationResult(true, fullUrl)
            } else {
                ValidationResult(false, fullUrl)
            }
        } else {
            ValidationResult(true, fullUrl)
        }
    }

    /**
     * Creates an object with the result and exposes the host in a format similar to what exists within the
     * list items.
     */
    class ValidationResult(val isDomainValid: Boolean, private val domain: String) {
        val host: String
            get() = getHost(domain)

        val getHost = { domain: String ->
            val host = URL(domain).host
            if (host.startsWith("www.")) {
                host.substring(4)
            } else {
                host
            }
        }
    }
}
