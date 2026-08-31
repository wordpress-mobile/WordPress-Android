package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import rs.wordpress.api.kotlin.toLogErrorString
import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.AllDomainsParams
import uniffi.wp_api.DomainSubtypeId
import javax.inject.Inject

class FetchAllDomainsUseCase @Inject constructor(
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
     * Fetches every domain across the account's sites, excluding the free
     * WordPress.com site addresses.
     *
     * The v1.1 endpoint took a `no_wpcom` query parameter for this. The v1.2
     * endpoint used here declares only `garden`, and the API framework
     * intersects the query string with the declared parameters before the
     * callback runs, so `no_wpcom` would be discarded. The exclusion therefore
     * happens client-side.
     *
     * `DefaultAddress` is the exact equivalent rather than an approximation:
     * the server assigns that subtype to precisely the domains `no_wpcom`
     * removed — the free `*.wordpress.com` address, along with the staging
     * (`*.wpcomstaging.com`) and garden subdomains it also surfaces as a site
     * address.
     */
    suspend fun execute(): AllDomains {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot fetch all domains without a WP.com access token"
            )
            return AllDomains.Error
        }
        val result = client
            .request { it.domains().allDomains(AllDomainsParams()).data }
        return when (result) {
            is WpRequestResult.Success -> {
                val domains = result.response.domains
                    .filterNot { it.subtype.id is DomainSubtypeId.DefaultAddress }
                if (domains.isEmpty()) {
                    AllDomains.Empty
                } else {
                    AllDomains.Success(domains)
                }
            }
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching all domains: " +
                        result.toLogErrorString()
                )
                AllDomains.Error
            }
        }
    }
}

sealed interface AllDomains {
    data class Success(
        val domains: List<AllDomainItem>,
    ) : AllDomains

    data object Empty : AllDomains

    data object Error : AllDomains
}
