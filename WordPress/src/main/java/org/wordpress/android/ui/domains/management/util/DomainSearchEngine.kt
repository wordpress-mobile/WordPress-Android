package org.wordpress.android.ui.domains.management.util

import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.util.LevenshteinDistanceAlgorithm
import javax.inject.Inject

class DomainSearchEngine @Inject constructor(private val levenshtein: LevenshteinDistanceAlgorithm) {
    fun filter(domains: List<AllDomainsDomain>, query: String): List<AllDomainsDomain> {
        return domains
            .mapNotNull { domain ->
                val name = domain.domain.orEmpty()
                val site = domain.siteSlug.orEmpty()
                val status = domain.domainStatus?.status.orEmpty()
                val maxSimilarity = maxOf(
                    levenshtein.levenshteinSimilarity(name, query),
                    levenshtein.levenshteinSimilarity(site, query),
                    levenshtein.levenshteinSimilarity(status, query)
                )
                if (maxSimilarity == 0.0) return@mapNotNull null
                domain to maxSimilarity
            }
            .sortedByDescending { (_, score) -> score }
            .map { (domain, _) -> domain }
            .toList()
    }
}
