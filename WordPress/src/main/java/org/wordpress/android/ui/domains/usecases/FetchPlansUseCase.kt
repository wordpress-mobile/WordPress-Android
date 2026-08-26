package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import rs.wordpress.api.kotlin.toLogErrorString
import uniffi.wp_api.ProductId
import uniffi.wp_api.SitePlan
import uniffi.wp_api.SitePlansParams
import javax.inject.Inject

class FetchPlansUseCase @Inject constructor(
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
     * Fetches the plans available to a site, keyed by product ID.
     */
    suspend fun execute(site: SiteModel): SitePlansResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot fetch site plans without a WP.com access token"
            )
            return SitePlansResult.Error
        }
        val result = client
            .request { it.sitePlans().list(site.siteId.toULong(), SitePlansParams()).data }
        return when (result) {
            is WpRequestResult.Success -> SitePlansResult.Success(result.response)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching site plans: " +
                        result.toLogErrorString()
                )
                SitePlansResult.Error
            }
        }
    }
}

sealed interface SitePlansResult {
    data class Success(
        val plans: Map<ProductId, SitePlan>,
    ) : SitePlansResult

    data object Error : SitePlansResult
}

/**
 * Whether the site holds an unclaimed free-domain credit.
 *
 * [SitePlan.currentPlan] is present on a plan when the response carries the
 * credit group for it, which the API sends for the plan the site is on. The
 * check does not depend on which plan that is: the response is a map, and its
 * iteration order is not stable across calls.
 *
 * A failed fetch reports no credit.
 */
fun SitePlansResult.hasDomainCredit(): Boolean = when (this) {
    is SitePlansResult.Success ->
        plans.values.any { it.currentPlan?.hasDomainCredit == true }
    is SitePlansResult.Error -> false
}
