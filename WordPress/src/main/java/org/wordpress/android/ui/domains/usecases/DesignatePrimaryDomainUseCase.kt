package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SetPrimaryDomainParams
import javax.inject.Inject

class DesignatePrimaryDomainUseCase @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
) {
    private var wpComApiClient: WpComApiClient? = null

    /**
     * Null when there is no WordPress.com account to make the request as.
     *
     * `AccountStore.accessToken` is typed nullable but reads `""` when signed
     * out, and is only null between an in-process sign out and the next
     * launch, so both have to be treated as no token.
     */
    @Synchronized
    private fun getOrCreateClient(): WpComApiClient? {
        val token = accountStore.accessToken?.takeIf { it.isNotEmpty() } ?: return null
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    /**
     * Makes [domain] the address [site] answers to.
     */
    suspend fun execute(site: SiteModel, domain: String): DesignatePrimaryDomainResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot designate a primary domain without a WP.com access token"
            )
            return DesignatePrimaryDomainResult.Error()
        }
        val siteId = wpComSiteIdOf(site)
        val params = setPrimaryDomainParams(domain)
        val result = client.request { it.domains().setPrimary(siteId, params).data }
        return when (result) {
            is WpRequestResult.Success -> DesignatePrimaryDomainResult.Success
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while designating the primary domain"
                )
                DesignatePrimaryDomainResult.Error(result.apiErrorMessage(), result.isDeviceOffline())
            }
        }
    }
}

/**
 * The site the request is addressed to. [SiteModel] also carries a local
 * database id, which this endpoint would take without complaint.
 */
internal fun wpComSiteIdOf(site: SiteModel): ULong = site.siteId.toULong()

/** The request body: the domain to make primary. */
internal fun setPrimaryDomainParams(domain: String) = SetPrimaryDomainParams(domain)

sealed interface DesignatePrimaryDomainResult {
    data object Success : DesignatePrimaryDomainResult

    data class Error(
        val message: String? = null,
        val isDeviceOffline: Boolean = false,
    ) : DesignatePrimaryDomainResult
}
