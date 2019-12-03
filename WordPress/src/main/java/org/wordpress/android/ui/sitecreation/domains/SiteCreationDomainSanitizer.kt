package org.wordpress.android.ui.sitecreation.domains

import java.util.Locale
import javax.inject.Inject

class SiteCreationDomainSanitizer
@Inject constructor() {
    /**
     * Sanitizes a query by taking the value before it's first period if it's present,
     * removes it's scheme and finally, removes any characters that aren't alphanumeric.
     */
    fun sanitizeDomainQuery(query: String) =
            query.run {
                val periodIndex = indexOf(".")
                if (periodIndex > 0) {
                    substring(0, periodIndex)
                } else {
                    this
                }
            }.replace("http://", "")
                    .replace("https://", "")
                    .replace("[^a-zA-Z0-9]".toRegex(), "")
                    .toLowerCase(Locale.ROOT)
}
