package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JetpackApplicationPasswordGenerator @Inject constructor(
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent),
    ApplicationPasswordGenerator {
    override suspend fun createApplicationPassword(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordCreationResult {
        AppLog.d(T.MAIN, "Create an application password using Jetpack Tunnel")

        val url = WPAPI.users.me.application_passwords.urlV2
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
            restClient = this,
            site = site,
            url = url,
            body = mapOf("name" to applicationName),
            clazz = ApplicationPasswordCreationResponse::class.java
        )

        return when (response) {
            is JetpackSuccess<ApplicationPasswordCreationResponse> -> {
                response.data?.let {
                    ApplicationPasswordCreationResult.Success(it.name)
                } ?: ApplicationPasswordCreationResult.Failure(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Password missing from response"
                    )
                )
            }
            is JetpackError<ApplicationPasswordCreationResponse> -> {
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
        AppLog.d(T.MAIN, "Delete application password using Jetpack Tunnel")

        val url = WPAPI.users.me.application_passwords.urlV2
        val response = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            restClient = this,
            site = siteModel,
            url = url,
            params = mapOf("name" to applicationName),
            clazz = ApplicationPasswordDeleteResponse::class.java
        )

        return when (response) {
            is JetpackSuccess<ApplicationPasswordDeleteResponse> -> {
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
            is JetpackError<ApplicationPasswordDeleteResponse> -> {
                val error = response.error
                AppLog.w(
                    T.MAIN, "Application password deletion failed, error: " +
                        "${error.type} ${error.apiError} ${error.message}\n" +
                        "${error.volleyError?.toString()}"
                )
                ApplicationPasswordDeletionResult.Failure(error)
            }
        }
    }
}