package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SetPrimaryDomainParams
import javax.inject.Inject

class DesignatePrimaryDomainUseCase @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    /**
     * Makes [domain] the address [site] answers to.
     */
    suspend fun execute(site: SiteModel, domain: String): DesignatePrimaryDomainResult {
        val siteId = wpComSiteIdOf(site)
        val params = setPrimaryDomainParams(domain)
        val result = wpComApiClient.request { it.domains().setPrimary(siteId, params).data }
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
