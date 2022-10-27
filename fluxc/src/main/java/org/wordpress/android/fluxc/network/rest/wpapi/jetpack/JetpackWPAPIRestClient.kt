package org.wordpress.android.fluxc.network.rest.wpapi.jetpack

import com.android.volley.Request
import com.android.volley.RequestQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpack.JetpackUser
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.RawRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIEncodedBodyRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class JetpackWPAPIRestClient @Inject constructor(
    private val wpApiEncodedBodyRequestBuilder: WPAPIEncodedBodyRequestBuilder,
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    @Named("no-redirects") private val noRedirectsRequestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun fetchJetpackConnectionUrl(
        site: SiteModel,
        nonce: Nonce?
    ): JetpackWPAPIPayload<String> {
        val baseUrl = site.wpApiRestUrl ?: "${site.url}/wp-json"
        val url = "${baseUrl.trimEnd('/')}/${JPAPI.connection.url.pathV4.trimStart('/')}"

        val response = wpApiEncodedBodyRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            nonce = nonce?.value
        )

        return when (response) {
            is Success<String> -> JetpackWPAPIPayload(response.data)
            is Error -> JetpackWPAPIPayload(response.error)
        }
    }

    suspend fun registerJetpackSite(registrationUrl: String): Result<String> {
        @Suppress("MagicNumber")
        fun Int.isRedirect(): Boolean = this in 300..399
        return suspendCancellableCoroutine { cont ->
            val request = RawRequest(
                method = Request.Method.GET,
                url = registrationUrl,
                listener = {
                    cont.resume(Result.failure(Exception("Got a success response instead of the expected redirect")))
                },
                onErrorListener = { error ->
                    val response = error.volleyError.networkResponse

                    if (response == null || !response.statusCode.isRedirect()) {
                        cont.resume(Result.failure(error.volleyError))
                        return@RawRequest
                    }

                    response.headers["Location"].let {
                        if (!it.isNullOrEmpty()) {
                            cont.resume(Result.success(response.headers["Location"]!!))
                        } else {
                            cont.resume(Result.failure(Exception("Location header missing")))
                        }
                    }
                }
            )

            noRedirectsRequestQueue.add(request)

            cont.invokeOnCancellation {
                request.cancel()
            }
        }
    }

    suspend fun fetchJetpackUser(
        site: SiteModel,
        nonce: Nonce?
    ): JetpackWPAPIPayload<JetpackUser> {
        val url = site.buildUrl(JPAPI.connection.data.pathV4)

        val response = wpApiGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            nonce = nonce?.value,
            clazz = JetpackConnectionDataResponse::class.java
        )

        return when (response) {
            is Success<JetpackConnectionDataResponse> -> JetpackWPAPIPayload(
                response.data?.let {
                    JetpackUser(
                        isConnected = it.currentUser.isConnected ?: false,
                        isMaster = it.currentUser.isMaster ?: false,
                        username = it.currentUser.username.orEmpty(),
                        wpcomEmail = it.currentUser.wpcomUser?.email.orEmpty(),
                        wpcomId = it.currentUser.wpcomUser?.id ?: 0L,
                        wpcomUsername = it.currentUser.wpcomUser?.login.orEmpty()
                    )
                }
            )
            is Error -> JetpackWPAPIPayload(response.error)
        }
    }

    private fun SiteModel.buildUrl(path: String): String {
        val baseUrl = wpApiRestUrl ?: "${url}/wp-json"
        return "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
    }

    data class JetpackWPAPIPayload<T>(
        val result: T?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null) {
            this.error = error
        }
    }
}
