package org.wordpress.android.ui.domains.management.util

import uniffi.wp_api.AllDomainItem
import javax.inject.Inject

class DomainLocalSearchEngine @Inject constructor() {
    fun filter(domains: List<AllDomainItem>, query: String): List<AllDomainItem> =
        domains.filter { it.matches(query) }

    private fun AllDomainItem.matches(query: String) =
        domain.contains(query, true)
                || siteSlug.contains(query, true)
                || blogName.contains(query, true)
                || domainStatus.label.contains(query, true)
}
