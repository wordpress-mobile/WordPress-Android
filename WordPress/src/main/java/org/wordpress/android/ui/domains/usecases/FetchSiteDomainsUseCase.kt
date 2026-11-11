package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SiteDomain
import javax.inject.Inject

class FetchSiteDomainsUseCase @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    /**
     * Fetches the domains attached to a single site, free WordPress.com address
     * included.
     */
    suspend fun execute(site: SiteModel): SiteDomainsResult {
        val result = wpComApiClient
            .request { it.domains().siteDomains(site.siteId.toULong()).data }
        return when (result) {
            is WpRequestResult.Success -> SiteDomainsResult.Success(result.response.domains)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching site domains"
                )
                SiteDomainsResult.Error
            }
        }
    }
}

sealed interface SiteDomainsResult {
    data class Success(
        val domains: List<SiteDomain>,
    ) : SiteDomainsResult

    data object Error : SiteDomainsResult
}
