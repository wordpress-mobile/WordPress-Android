package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import rs.wordpress.api.kotlin.toLogErrorString
import uniffi.wp_api.SiteDomain
import javax.inject.Inject

class FetchSiteDomainsUseCase @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
) {
    private var wpComApiClient: WpComApiClient? = null

    @Synchronized
    private fun getOrCreateClient(): WpComApiClient? {
        val token = accountStore.accessToken?.takeIf { it.isNotEmpty() } ?: return null
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    /**
     * Fetches the domains attached to a single site, free WordPress.com address
     * included.
     */
    suspend fun execute(site: SiteModel): SiteDomainsResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot fetch site domains without a WP.com access token"
            )
            return SiteDomainsResult.Error
        }
        val result = client
            .request { it.domains().siteDomains(site.siteId.toULong()).data }
        return when (result) {
            is WpRequestResult.Success -> SiteDomainsResult.Success(result.response.domains)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching site domains: " +
                        result.toLogErrorString()
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
