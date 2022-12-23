package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
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
internal class JetpackApplicationPasswordsRestClient @Inject constructor(
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun createApplicationPassword(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordCreationPayload {
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
                    ApplicationPasswordCreationPayload(it.password)
                } ?: ApplicationPasswordCreationPayload(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Password missing from response"
                    )
                )
            }
            is JetpackError<ApplicationPasswordCreationResponse> ->
                ApplicationPasswordCreationPayload(response.error)
        }
    }

    suspend fun deleteApplicationPassword(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordDeletionPayload {
        AppLog.d(T.MAIN, "Delete application password using Jetpack Tunnel")

        val url = WPAPI.users.me.application_passwords.urlV2
        val response = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            restClient = this,
            site = site,
            url = url,
            params = mapOf("name" to applicationName),
            clazz = ApplicationPasswordDeleteResponse::class.java
        )

        return when (response) {
            is JetpackSuccess<ApplicationPasswordDeleteResponse> -> {
                response.data?.let {
                    ApplicationPasswordDeletionPayload(it.deleted)
                } ?: ApplicationPasswordDeletionPayload(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Response is empty"
                    )
                )
            }
            is JetpackError<ApplicationPasswordDeleteResponse> -> {
                ApplicationPasswordDeletionPayload(response.error)
            }
        }
    }

    suspend fun fetchWPAdminUsername(
        site: SiteModel
    ): UsernameFetchPayload {
        AppLog.d(T.MAIN, "Fetch wp-admin username using Jetpack Tunnel")

        val url = WPAPI.users.me.urlV2

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            restClient = this,
            site = site,
            url = url,
            params = mapOf(
                "_fields" to "username",
                "context" to "edit"
            ),
            clazz = UserApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess<UserApiResponse> -> {
                response.data?.let {
                    UsernameFetchPayload(it.username)
                } ?: UsernameFetchPayload(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Response is empty"
                    )
                )
            }
            is JetpackError<UserApiResponse> -> {
                UsernameFetchPayload(response.error)
            }
        }
    }

    private data class UserApiResponse(
        @SerializedName("username") val username: String
    )
}
