package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ProductId
import uniffi.wp_api.SitePlan
import uniffi.wp_api.SitePlansParams
import javax.inject.Inject

class FetchPlansUseCase @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    /**
     * Fetches the plans available to a site, keyed by product ID.
     */
    suspend fun execute(site: SiteModel): SitePlansResult {
        val result = wpComApiClient
            .request { it.sitePlans().list(site.siteId.toULong(), SitePlansParams()).data }
        return when (result) {
            is WpRequestResult.Success -> SitePlansResult.Success(result.response)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching site plans"
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
 * Whether the site's current plan holds an unclaimed free-domain credit.
 *
 * [SitePlan.currentPlan] is set on the plan the site is on and carries the
 * credit flag.
 *
 * This assumes a site has one current plan. Nothing here can enforce that, so
 * if it ever stops holding, this answer should be discarded along with it. In
 * that case a credit on any plan reporting itself as current counts. The
 * server decides whether a credit can actually be claimed, so reporting one
 * that is not there costs a call to action the purchase flow refuses, while
 * missing one that is there would leave the user no way to reach it.
 *
 * Only a fetched set of plans can answer this. A caller holding a
 * [SitePlansResult.Error] knows nothing about the credit, which is a different
 * thing from knowing there is none, so the receiver is the success case alone.
 */
fun SitePlansResult.Success.hasDomainCredit(): Boolean =
    plans.values.any { it.currentPlan?.hasDomainCredit == true }
