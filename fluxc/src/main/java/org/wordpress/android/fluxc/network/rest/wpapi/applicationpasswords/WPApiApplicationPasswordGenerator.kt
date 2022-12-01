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
class WPApiApplicationPasswordGenerator @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val wpApiAuthenticator: WPAPIAuthenticator,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent),
    ApplicationPasswordGenerator {
    override suspend fun createApplicationPassword(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordCreationResult {
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
                    ApplicationPasswordCreationResult.Success(it.name)
                } ?: ApplicationPasswordCreationResult.Failure(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Password missing from response"
                    )
                )
            }
            is WPAPIResponse.Error<ApplicationPasswordCreationResponse> -> {
                when (response.error.volleyError?.networkResponse?.statusCode) {
                    409 -> {
                        AppLog.w(T.MAIN, "Application Password already exists")
                        when (val deletionResult = deleteApplicationPassword(site, applicationName)) {
                            ApplicationPasswordDeletionResult.Success ->
                                createApplicationPassword(site, applicationName)
                            is ApplicationPasswordDeletionResult.Failure ->
                                ApplicationPasswordCreationResult.Failure(deletionResult.error)
                        }
                    }
                    404 -> {
                        AppLog.w(T.MAIN, "Application Password feature not supported")
                        ApplicationPasswordCreationResult.NotSupported
                    }
                    else -> {
                        AppLog.w(
                            T.MAIN,
                            "Application Password creation failed ${response.error.type}"
                        )
                        ApplicationPasswordCreationResult.Failure(response.error)
                    }
                }
            }
        }
    }

    override suspend fun deleteApplicationPassword(
        siteModel: SiteModel,
        applicationName: String
    ): ApplicationPasswordDeletionResult {
        AppLog.d(T.MAIN, "Delete application password using Cookie Authentication")

        val path = WPAPI.users.me.application_passwords.urlV2
        val payload = wpApiAuthenticator.makeAuthenticatedWPAPIRequest(siteModel) { nonce ->
            APIResponseWrapper(
                wpApiGsonRequestBuilder.syncDeleteRequest(
                    restClient = this,
                    url = siteModel.buildUrl(path),
                    body = mapOf("name" to applicationName),
                    clazz = ApplicationPasswordDeleteResponse::class.java,
                    nonce = nonce?.value
                )
            )
        }

        return when (val response = payload.response) {
            is WPAPIResponse.Success<ApplicationPasswordDeleteResponse> -> {
                if (response.data?.deleted == true) {
                    AppLog.d(T.MAIN, "Application password deleted")
                    ApplicationPasswordDeletionResult.Success
                } else {
                    AppLog.w(T.MAIN, "Application password deletion failed")
                    ApplicationPasswordDeletionResult.Failure(
                        BaseNetworkError(
                            GenericErrorType.UNKNOWN,
                            "Deletion not confirmed by API"
                        )
                    )
                }
            }
            is WPAPIResponse.Error<ApplicationPasswordDeleteResponse> -> {
                val error = response.error
                AppLog.w(
                    T.MAIN, "Application password deletion failed, error: " +
                        "${error.type} ${error.message}\n" +
                        "${error.volleyError?.toString()}"
                )
                ApplicationPasswordDeletionResult.Failure(error)
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