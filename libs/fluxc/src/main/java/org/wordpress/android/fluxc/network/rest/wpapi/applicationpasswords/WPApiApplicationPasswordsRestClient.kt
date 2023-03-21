package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.Request
import com.android.volley.RequestQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Credentials
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
internal class WPApiApplicationPasswordsRestClient @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val cookieNonceAuthenticator: CookieNonceAuthenticator,
    @Named("no-cookies") private val noCookieRequestQueue: RequestQueue,
    @Named("regular") requestQueue: RequestQueue,
    dispatcher: Dispatcher,
    private val userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun createApplicationPassword(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordCreationPayload {
        AppLog.d(T.MAIN, "Create an application password using Cookie Authentication")
        val path = WPAPI.users.me.application_passwords.urlV2

        val response = cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncPostRequest(
                restClient = this,
                url = site.buildUrl(path),
                body = mapOf("name" to applicationName),
                clazz = ApplicationPasswordCreationResponse::class.java,
                nonce = nonce.value
            )
        }

        return when (response) {
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

    suspend fun fetchApplicationPasswordUUID(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordUUIDFetchPayload {
        AppLog.d(T.MAIN, "Fetch application password UUID using Cookie authentication")
        val path = WPAPI.users.me.application_passwords.urlV2
        val response = cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.buildUrl(path),
                clazz = Array<ApplicationPasswordsFetchResponse>::class.java,
                nonce = nonce.value
            )
        }

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.firstOrNull { it.name == applicationName }?.let {
                    ApplicationPasswordUUIDFetchPayload(it.uuid)
                } ?: ApplicationPasswordUUIDFetchPayload(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "UUID for application password $applicationName was not found"
                    )
                )
            }

            is WPAPIResponse.Error -> ApplicationPasswordUUIDFetchPayload(response.error)
        }
    }

    suspend fun deleteApplicationPassword(
        site: SiteModel,
        uuid: ApplicationPasswordUUID
    ): ApplicationPasswordDeletionPayload {
        AppLog.d(T.MAIN, "Delete application password")
        val path = WPAPI.users.me.application_passwords.uuid(uuid).urlV2

        val response = cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncDeleteRequest(
                restClient = this,
                url = site.buildUrl(path),
                clazz = ApplicationPasswordDeleteResponse::class.java,
                nonce = nonce.value
            )
        }

        return response.toPayload()
    }

    suspend fun deleteApplicationPassword(
        site: SiteModel,
        credentials: ApplicationPasswordCredentials
    ): ApplicationPasswordDeletionPayload {
        AppLog.d(T.MAIN, "Delete application password using Basic Authentication")

        val uuid = credentials.uuid ?: fetchApplicationPasswordUsingBasicAuth(site, credentials).let {
            if (it is WPAPIResponse.Success && it.data != null) {
                it.data
            } else {
                return ApplicationPasswordDeletionPayload((it as WPAPIResponse.Error).error)
            }
        }

        return deleteApplicationPasswordUsingBasicAuth(site, credentials, uuid).toPayload()
    }

    private suspend fun fetchApplicationPasswordUsingBasicAuth(
        site: SiteModel,
        applicationPasswordCredentials: ApplicationPasswordCredentials
    ): WPAPIResponse<ApplicationPasswordUUID> {
        AppLog.d(T.MAIN, "Fetching application password UUID using the /introspect endpoint")

        val path = WPAPI.users.me.application_passwords.introspect.urlV2

        val response = invokeRequestUsingBasicAuth<ApplicationPasswordsFetchResponse>(
            site = site,
            path = path,
            credentials = applicationPasswordCredentials,
            method = Request.Method.GET,
        )

        @Suppress("UNCHECKED_CAST")
        return when (response) {
            is WPAPIResponse.Success -> response.data?.let {
                WPAPIResponse.Success(it.uuid)
            } ?: WPAPIResponse.Error(
                WPAPINetworkError(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Response is empty"
                    )
                )
            )

            is WPAPIResponse.Error -> response as WPAPIResponse.Error<ApplicationPasswordUUID>
        }
    }

    private suspend fun deleteApplicationPasswordUsingBasicAuth(
        site: SiteModel,
        credentials: ApplicationPasswordCredentials,
        uuid: ApplicationPasswordUUID
    ): WPAPIResponse<ApplicationPasswordDeleteResponse> {
        val path = WPAPI.users.me.application_passwords.uuid(uuid).urlV2

        return invokeRequestUsingBasicAuth(
            site = site,
            credentials = credentials,
            path = path,
            method = Request.Method.DELETE
        )
    }

    private fun WPAPIResponse<ApplicationPasswordDeleteResponse>.toPayload() = when (this) {
        is WPAPIResponse.Success<ApplicationPasswordDeleteResponse> -> {
            data?.let {
                ApplicationPasswordDeletionPayload(it.deleted)
            } ?: ApplicationPasswordDeletionPayload(
                BaseNetworkError(
                    GenericErrorType.UNKNOWN,
                    "Response is empty"
                )
            )
        }

        is WPAPIResponse.Error<ApplicationPasswordDeleteResponse> -> {
            ApplicationPasswordDeletionPayload(error)
        }
    }

    private suspend inline fun <reified T> invokeRequestUsingBasicAuth(
        site: SiteModel,
        credentials: ApplicationPasswordCredentials,
        path: String,
        method: Int
    ): WPAPIResponse<T> {
        return suspendCancellableCoroutine { continuation ->
            val request = WPAPIGsonRequest(
                method,
                site.buildUrl(path),
                emptyMap(),
                emptyMap(),
                T::class.java,
                /* listener = */ { continuation.resume(WPAPIResponse.Success(it)) },
                /* errorListener = */ { continuation.resume(WPAPIResponse.Error(it)) }
            )

            request.addHeader("Authorization", Credentials.basic(credentials.userName, credentials.password))
            request.setUserAgent(userAgent.userAgent)

            noCookieRequestQueue.add(request)
        }
    }

    private fun SiteModel.buildUrl(path: String): String {
        val baseUrl = wpApiRestUrl ?: url.slashJoin("/wp-json")
        return baseUrl.slashJoin(path)
    }
}
