package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.dashboard.CardsStore.FetchedCardsPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CardsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun fetchCards(site: SiteModel) = FetchedCardsPayload(CardsResponse())

    data class CardsResponse(
        @SerializedName("todo") val todo: Int = 0
    )
}
