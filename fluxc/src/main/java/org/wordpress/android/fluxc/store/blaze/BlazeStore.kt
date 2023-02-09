package org.wordpress.android.fluxc.store.blaze

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusFetchedPayload
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao.BlazeStatus
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlazeStore @Inject constructor(
    private val blazeRestClient: BlazeRestClient,
    private val blazeStatusDao: BlazeStatusDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchBlazeStatus(
        site: SiteModel
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch blaze status") {
        val payload = blazeRestClient.fetchBlazeStatus(site)
        storeBlazeStatus(site, payload)
    }

    private fun storeBlazeStatus(
        site: SiteModel,
        payload: BlazeStatusFetchedPayload
    ): BlazeStatusResult {
        return when {
            payload.isError -> handlePayloadError(payload.error)
            payload.eligibility != null -> handlePayloadSuccess(site, payload.eligibility)
            else -> BlazeStatusResult(BlazeStatusError((INVALID_RESPONSE)))
        }
    }

    private fun handlePayloadSuccess(site: SiteModel, eligibility: Map<String, Boolean>?): BlazeStatusResult {
        val isEligible = eligibility?.get("approved")?.toString()?.toBoolean()?:false
        insertBlazeStatusValue(site.siteId, isEligible)
        return BlazeStatusResult(isEligible)
    }

    private fun handlePayloadError(error: BlazeStatusError): BlazeStatusResult {
        return BlazeStatusResult(error)
    }

    private fun insertBlazeStatusValue(siteId: Long, value: Boolean) {
        blazeStatusDao.insert(
            BlazeStatus(
                siteId = siteId,
                isEligible = value
            )
        )
    }

    fun isSiteEligibleForBlaze(siteId: Long): Boolean {
        return blazeStatusDao.getBlazeStatus(siteId).takeIf { it.isNotEmpty() }?.first()?.isEligible
            ?: false
    }

    fun clear() {
        blazeStatusDao.clear()
    }

    data class BlazeStatusResult(
        val isEligible: Boolean = false
    ) : Store.OnChanged<BlazeStatusError>() {
        constructor(error: BlazeStatusError) : this() {
            this.error = error
        }
    }
}
