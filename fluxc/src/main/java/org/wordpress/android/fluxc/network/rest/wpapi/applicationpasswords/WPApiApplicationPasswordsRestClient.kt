package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class WPApiApplicationPasswordsRestClient @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val wpApiAuthenticator: WPAPIAuthenticator,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun createApplicationPassword(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordCreationPayload {
        AppLog.d(T.MAIN, "Create an application password using Cookie Authentication")
        val path = WPAPI.users.me.application_passwords.urlV2

        val payload = wpApiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            APIResponseWrapper(
                wpApiGsonRequestBuilder.syncPostRequest(
                    restClient = this,
                    url = site.buildUrl(path),
                    body = mapOf("name" to applicationName),
                    clazz = ApplicationPasswordCreationResponse::class.java,
                    nonce = nonce?.value
                )
            )
        }

        return when (val response = payload.response) {
            is WPAPIResponse.Success<ApplicationPasswordCreationResponse> -> {
                response.data?.let {
                    ApplicationPasswordCreationPayload(it.password, it.uuid)
                } ?: ApplicationPasswordCreationPayload(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Password missing from response"
                    )
                )
            }
            is WPAPIResponse.Error<ApplicationPasswordCreationResponse> ->
                ApplicationPasswordCreationPayload(response.error)
        }
    }

    suspend fun deleteApplicationPassword(
        site: SiteModel,
        uuid: String
    ): ApplicationPasswordDeletionPayload {
        AppLog.d(T.MAIN, "Delete application password using Cookie Authentication")

        val path = WPAPI.users.me.application_passwords.uuid(uuid).urlV2
        val payload = wpApiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            APIResponseWrapper(
                wpApiGsonRequestBuilder.syncDeleteRequest(
                    restClient = this,
                    url = site.buildUrl(path),
                    clazz = ApplicationPasswordDeleteResponse::class.java,
                    nonce = nonce?.value
                )
            )
        }

        return when (val response = payload.response) {
            is WPAPIResponse.Success<ApplicationPasswordDeleteResponse> -> {
                response.data?.let {
                    ApplicationPasswordDeletionPayload(it.deleted)
                } ?: ApplicationPasswordDeletionPayload(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Response is empty"
                    )
                )
            }
            is WPAPIResponse.Error<ApplicationPasswordDeleteResponse> -> {
                ApplicationPasswordDeletionPayload(response.error)
            }
        }
    }

    private fun SiteModel.buildUrl(path: String): String {
        val baseUrl = wpApiRestUrl ?: "${url}/wp-json"
        return "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
    }

    private data class APIResponseWrapper<T>(val response: WPAPIResponse<T>) : Payload<BaseNetworkError?>() {
        init {
            if (response is WPAPIResponse.Error) {
                this.error = response.error
            }
        }
    }
}
