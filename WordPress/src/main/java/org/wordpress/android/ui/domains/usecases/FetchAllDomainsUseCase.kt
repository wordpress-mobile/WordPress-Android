package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.AllDomainsParams
import javax.inject.Inject

class FetchAllDomainsUseCase @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
) {
    private var wpComApiClient: WpComApiClient? = null

    @Synchronized
    private fun getOrCreateClient(): WpComApiClient {
        val token = requireNotNull(accountStore.accessToken) {
            "WP.com access token is required"
        }
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    suspend fun execute(): AllDomains {
        val result = getOrCreateClient()
            .request { it.domains().allDomains(AllDomainsParams()).data }
        return when (result) {
            is WpRequestResult.Success -> {
                val domains = result.response.domains
                if (domains.isEmpty()) {
                    AllDomains.Empty
                } else {
                    AllDomains.Success(domains)
                }
            }
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching all domains"
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
