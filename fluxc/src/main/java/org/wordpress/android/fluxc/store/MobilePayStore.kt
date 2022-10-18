package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.mobilepay.MobilePayRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject

class MobilePayStore @Inject constructor(
    private val restClient: MobilePayRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun createOrder(
        productIdentifier: String,
        price: Long,
        currency: String,
        purchaseToken: String,
        appId: String,
        site: SiteModel,
    ) = coroutineEngine.withDefaultContext(API, this, "createOrder") {
        restClient.createOrder(
            productIdentifier,
            price,
            currency,
            purchaseToken,
            appId,
            site,
        )
    }
}