package org.wordpress.android.ui.domains.management.util

import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.util.LevenshteinDistanceAlgorithm
import javax.inject.Inject

class DomainSearchEngine @Inject constructor(private val levenshtein: LevenshteinDistanceAlgorithm) {
    fun filter(query: String, allDomainsDomains: List<AllDomainsDomain>): List<AllDomainsDomain> {
        val threshold = 0.5
        val optimizedQuery = query.optimize()
        val domainScores = allDomainsDomains.map { domain ->
            val name = domain.domain.orEmpty()
            val site = domain.siteSlug.orEmpty()
            val status = domain.domainStatus?.status.orEmpty()
            val maxSimilarity = maxOf(
                levenshtein.levenshteinSimilarity(name, optimizedQuery),
                levenshtein.levenshteinSimilarity(site, optimizedQuery),
                levenshtein.levenshteinSimilarity(status, optimizedQuery)
            )
            domain to maxSimilarity // Pair domain with its max similarity score
        }

        // Now filter by threshold and sort by the stored scores in descending order
        return domainScores.asSequence()
            .filter { (_, score) -> score >= threshold }
            .sortedByDescending { (_, score) -> score }
            .map { (domain, _) -> domain } // Extract the domain from the pair
            .toList()
    }

    private fun String.optimize(): String = trim()
}
