package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JetpackAIRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String
    ): JetpackAICompletionsPayload {
        val url = WPCOMV2.sites.site(site.siteId).jetpack_ai.completions.url
        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = emptyMap(),
            body = mapOf(
                "content" to prompt
            ),
            clazz = String::class.java
        )

        return when (response) {
            is Response.Success -> JetpackAICompletionsPayload.Success(response.data)
            is Response.Error -> JetpackAICompletionsPayload.Error(response.error)
        }
    }
}

sealed class JetpackAICompletionsPayload {
    data class Success(val completions: String) : JetpackAICompletionsPayload()
    class Error(val error: BaseNetworkError?) : JetpackAICompletionsPayload()
}
