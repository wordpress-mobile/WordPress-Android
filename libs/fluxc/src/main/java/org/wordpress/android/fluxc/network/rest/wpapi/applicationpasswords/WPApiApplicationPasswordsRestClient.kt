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
    private val wpApiAuthenticator: CookieNonceAuthenticator,
    private val applicationPasswordsStore: ApplicationPasswordsStore,
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

        val response = wpApiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
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
        val response = wpApiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
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

        val applicationPasswordCredentials = applicationPasswordsStore.getCredentials(site)
        val response = if (applicationPasswordCredentials != null) {
            // If we have the credentials, attempt deleting the password using them
            // This will allow deleting the password for sites authorized using the web flow where we don't have
            // the user's credentials
            deleteApplicationPasswordUsingBasicAuth(site, applicationPasswordCredentials)
        } else {
            deleteApplicationPasswordUsingCookieAuth(site, uuid)
        }

        return when (response) {
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

    private suspend fun deleteApplicationPasswordUsingBasicAuth(
        site: SiteModel,
        credentials: ApplicationPasswordCredentials
    ) = suspendCancellableCoroutine<WPAPIResponse<ApplicationPasswordDeleteResponse>> { continuation ->
        val path = WPAPI.users.me.application_passwords.uuid(credentials.uuid).urlV2

        val request = WPAPIGsonRequest(
            Request.Method.DELETE,
            site.buildUrl(path),
            emptyMap(),
            emptyMap(),
            ApplicationPasswordDeleteResponse::class.java,
            /* listener = */ { continuation.resume(WPAPIResponse.Success(it)) },
            /* errorListener = */ { continuation.resume(WPAPIResponse.Error(it)) }
        )

        request.addHeader("Authorization", Credentials.basic(credentials.userName, credentials.password))
        request.setUserAgent(userAgent.userAgent)

        noCookieRequestQueue.add(request)
    }

    private suspend fun deleteApplicationPasswordUsingCookieAuth(
        site: SiteModel,
        uuid: ApplicationPasswordUUID
    ): WPAPIResponse<ApplicationPasswordDeleteResponse> {
        val path = WPAPI.users.me.application_passwords.uuid(uuid).urlV2

        return wpApiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncDeleteRequest(
                restClient = this,
                url = site.buildUrl(path),
                clazz = ApplicationPasswordDeleteResponse::class.java,
                nonce = nonce.value
            )
        }
    }

    private fun SiteModel.buildUrl(path: String): String {
        val baseUrl = wpApiRestUrl ?: url.slashJoin("/wp-json")
        return baseUrl.slashJoin(path)
    }
}
