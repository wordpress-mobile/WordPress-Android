package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.HtmlUtils
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val NOT_FOUND_STATUS_CODE = 404

@Singleton
class NonceRestClient @Inject constructor(
    private val wpApiEncodedBodyRequestBuilder: WPAPIEncodedBodyRequestBuilder,
    private val currentTimeProvider: CurrentTimeProvider,
    dispatcher: Dispatcher,
    @Named("no-redirects") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    private val nonceMap: MutableMap<String, Nonce> = mutableMapOf()
    fun getNonce(siteUrl: String, username: String?): Nonce? = nonceMap[siteUrl]?.takeIf { it.username == username }
    fun getNonce(site: SiteModel): Nonce? = getNonce(site.url, site.username)

    /**
     *  Requests a nonce using the
     *  [rest-nonce endpoint](https://developer.wordpress.org/reference/functions/wp_ajax_rest_nonce/)
     *  that became available in WordPress 5.3.
     */
    suspend fun requestNonce(site: SiteModel): Nonce {
        if (site.username == null || site.password == null) return Unknown(site.username)
        return requestNonce(site.url, site.username, site.password)
    }

    /**
     *  Requests a nonce using the
     *  [rest-nonce endpoint](https://developer.wordpress.org/reference/functions/wp_ajax_rest_nonce/)
     *  that became available in WordPress 5.3.
     */
    @Suppress("NestedBlockDepth")
    suspend fun requestNonce(siteUrl: String, username: String, password: String): Nonce {
        @Suppress("MagicNumber")
        fun Int.isRedirect(): Boolean = this in 300..399
        val wpLoginUrl = siteUrl.slashJoin("wp-login.php")
        val redirectUrl = siteUrl.slashJoin("wp-admin/admin-ajax.php?action=rest-nonce")
        val body = mapOf(
            "log" to username,
            "pwd" to password,
            "redirect_to" to redirectUrl
        )
        val response =
            wpApiEncodedBodyRequestBuilder.syncPostRequest(this, wpLoginUrl, body = body)
        val nonce = when (response) {
            is Success -> {
                // A success means we got 200 from the wp-login.php call, which means
                // a login error: https://core.trac.wordpress.org/ticket/25446
                // A successful login should result in a redirection to the redirect URL

                // Let's try to extract the login error from the web page, and if we have it, then we'll assume
                // that it's an authentication issue, otherwise we'll assume it's an invalid response
                val errorMessage = extractErrorMessage(response.data.orEmpty())

                val errorType = if (hasInvalidCredentialsPattern(response.data.orEmpty())) {
                    Nonce.CookieNonceErrorType.INVALID_CREDENTIALS
                } else if (errorMessage != null) {
                    Nonce.CookieNonceErrorType.NOT_AUTHENTICATED
                } else {
                    Nonce.CookieNonceErrorType.INVALID_RESPONSE
                }

                FailedRequest(
                    timeOfResponse = currentTimeProvider.currentDate().time,
                    username = username,
                    type = errorType,
                    errorMessage = errorMessage
                )
            }

            is Error -> {
                if (response.error.volleyError is NoConnectionError) {
                    // No connection, so we do not know if a nonce is available
                    Unknown(username)
                } else {
                    val networkResponse = response.error.volleyError?.networkResponse
                    if (networkResponse?.statusCode?.isRedirect() == true) {
                        requestNonce(networkResponse.headers?.get("Location") ?: redirectUrl, username)
                    } else {
                        FailedRequest(
                            timeOfResponse = currentTimeProvider.currentDate().time,
                            username = username,
                            type = if (networkResponse?.statusCode == NOT_FOUND_STATUS_CODE) {
                                Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL
                            } else Nonce.CookieNonceErrorType.GENERIC_ERROR,
                            networkError = response.error,
                            errorMessage = response.error.message,
                        )
                    }
                }
            }
        }
        return nonce.also {
            nonceMap[siteUrl] = it
        }
    }

    private suspend fun requestNonce(redirectUrl: String, username: String): Nonce {
        return when (
            val response = wpApiEncodedBodyRequestBuilder.syncGetRequest(this, redirectUrl)
        ) {
            is Success -> {
                if (response.data?.matches("[0-9a-zA-Z]{2,}".toRegex()) == true) {
                    Available(value = response.data, username = username)
                } else {
                    FailedRequest(
                        timeOfResponse = currentTimeProvider.currentDate().time,
                        username = username,
                        type = Nonce.CookieNonceErrorType.INVALID_NONCE
                    )
                }
            }

            is Error -> {
                val statusCode = response.error.volleyError?.networkResponse?.statusCode
                FailedRequest(
                    timeOfResponse = currentTimeProvider.currentDate().time,
                    username = username,
                    type = if (statusCode == NOT_FOUND_STATUS_CODE) Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL
                    else Nonce.CookieNonceErrorType.GENERIC_ERROR,
                    networkError = response.error,
                    errorMessage = response.error.message,
                )
            }
        }
    }

    private fun extractErrorMessage(htmlResponse: String): String? {
        val regex = Regex("<div[^>]*id=\"login_error\"[^>]*>([\\s\\S]+?)</div>")
        val loginErrorDiv = regex.find(htmlResponse)?.groupValues?.get(1) ?: return null
        val urlRegex = Regex("<a[^>]*href=\".*\"[^>]*>[\\s\\S]+?</a>")

        val errorHtml = loginErrorDiv.replace(urlRegex, "")
        // Strip HTML tags
        return HtmlUtils.fastStripHtml(errorHtml)
            .trim(' ', '\n')
    }

    private fun hasInvalidCredentialsPattern(htmlResponse: String) =
        htmlResponse.contains(INVALID_CREDENTIAL_HTML_PATTERN)

    companion object {
        const val INVALID_CREDENTIAL_HTML_PATTERN = "document.querySelector('form').classList.add('shake')"
    }
}
