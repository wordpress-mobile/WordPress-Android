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
     *
     * The response carries a `success` flag the server hardcodes to true, so a
     * refusal — no paid plan, a domain that is not active, one the account does
     * not own — arrives as an HTTP error instead.
     */
    suspend fun execute(site: SiteModel, domain: String): DesignatePrimaryDomainResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot designate a primary domain without a WP.com access token"
            )
            return DesignatePrimaryDomainResult.Error()
        }
        val params = SetPrimaryDomainParams(domain)
        val result = client.request { it.domains().setPrimary(site.siteId.toULong(), params).data }
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

sealed interface DesignatePrimaryDomainResult {
    data object Success : DesignatePrimaryDomainResult

    data class Error(
        val message: String? = null,
        val isDeviceOffline: Boolean = false,
    ) : DesignatePrimaryDomainResult
}
