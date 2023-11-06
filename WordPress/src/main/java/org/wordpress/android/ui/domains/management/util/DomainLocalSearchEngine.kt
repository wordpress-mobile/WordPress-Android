package org.wordpress.android.ui.domains.management.util

import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import javax.inject.Inject

class DomainLocalSearchEngine @Inject constructor() {
    fun filter(domains: List<AllDomainsDomain>, query: String): List<AllDomainsDomain> =
        domains.filter { it.matches(query) }

    private fun AllDomainsDomain.matches(query: String) =
        domain?.contains(query, true) ?: false
                || siteSlug?.contains(query, true) ?: false
                || domainStatus?.status?.contains(query, true) ?: false
}
