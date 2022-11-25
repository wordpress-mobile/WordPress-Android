package org.wordpress.android.fluxc.network.rest.wpcom.mobile

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.mobile.MigrationCompleteFetchedPayload
import org.wordpress.android.fluxc.store.mobile.MigrationCompleteFetchedPayload.Success
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JetpackMigrationRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun migrationComplete(
        errorHandler: (error: BaseNetworkError?) -> MigrationCompleteFetchedPayload,
    ): MigrationCompleteFetchedPayload {
        // https://public-api.wordpress.com/wpcom/v2/mobile/migration
        val url = WPCOMV2.mobile.migration.url
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                mapOf(),
                mapOf(),
                JsonElement::class.java
        )
        return when (response) {
            is Response.Success -> Success
            is Response.Error -> errorHandler(response.error)
        }
    }
}
