package org.wordpress.android.fluxc.network.rest.wpapi.taxonomy

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel

interface TaxonomyRsClient {
    fun fetchTerms(site: SiteModel, taxonomyName: String)
    fun createTerm(site: SiteModel, term: TermModel)
    fun updateTerm(site: SiteModel, term: TermModel)
    fun deleteTerm(site: SiteModel, term: TermModel)
}
