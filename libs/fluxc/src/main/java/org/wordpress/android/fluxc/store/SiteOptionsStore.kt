package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.StaticPage
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.CENSORED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType.INVALID_PARAMETERS
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

class SiteOptionsStore
@Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val siteHomepageRestClient: SiteHomepageRestClient
) {
    suspend fun updatePageForPosts(site: SiteModel, pageForPostsId: Long): HomepageUpdatedPayload =
            coroutineEngine.withDefaultContext(T.API, this, "Update page for posts") {
                if (site.pageForPosts == pageForPostsId) {
                    return@withDefaultContext HomepageUpdatedPayload(
                            SiteOptionsError(
                                    GENERIC_ERROR,
                                    "Trying to set pageForPosts with an already set value"
                            )
                    )
                }
                val updatedPageOnFrontId = if (pageForPostsId == site.pageOnFront) {
                    0
                } else {
                    site.pageOnFront
                }
                val updatedHomepageSettings = StaticPage(
                        pageForPostsId = pageForPostsId,
                        pageOnFrontId = updatedPageOnFrontId
                )
                return@withDefaultContext updateHomepage(site, updatedHomepageSettings)
            }

    suspend fun updatePageOnFront(site: SiteModel, pageOnFrontId: Long): HomepageUpdatedPayload =
            coroutineEngine.withDefaultContext(T.API, this, "Update page on front") {
                if (site.pageOnFront == pageOnFrontId) {
                    return@withDefaultContext HomepageUpdatedPayload(
                            SiteOptionsError(
                                    GENERIC_ERROR,
                                    "Trying to set pageOnFront with an already set value"
                            )
                    )
                }
                val updatedPageForPostsId = if (pageOnFrontId == site.pageForPosts) {
                    0
                } else {
                    site.pageForPosts
                }
                val updatedHomepageSettings = StaticPage(
                        pageForPostsId = updatedPageForPostsId,
                        pageOnFrontId = pageOnFrontId
                )
                return@withDefaultContext updateHomepage(site, updatedHomepageSettings)
            }

    suspend fun updateHomepage(site: SiteModel, homepageSettings: SiteHomepageSettings): HomepageUpdatedPayload =
            coroutineEngine.withDefaultContext(T.API, this, "Update homepage settings") {
                if (homepageSettings is StaticPage &&
                        homepageSettings.pageForPostsId == homepageSettings.pageOnFrontId) {
                    return@withDefaultContext HomepageUpdatedPayload(
                            SiteOptionsError(
                                    INVALID_PARAMETERS,
                                    "Page for posts and page on front cannot be the same"
                            )
                    )
                }
                return@withDefaultContext if (site.isUsingWpComRestApi) {
                    siteHomepageRestClient.updateHomepage(site, homepageSettings)
                } else {
                    HomepageUpdatedPayload(
                            SiteOptionsError(
                                    GENERIC_ERROR,
                                    "You cannot update homepage for a self-hosted site"
                            )
                    )
                }
            }

    data class HomepageUpdatedPayload(
        val homepageSettings: SiteHomepageSettings? = null
    ) : Payload<SiteOptionsError>() {
        constructor(error: BaseNetworkError) : this() {
            this.error = error.toSiteOptionsError()
        }

        constructor(errorType: SiteOptionsError) : this() {
            this.error = errorType
        }
    }

    data class SiteOptionsError(var type: SiteOptionsErrorType, var message: String? = null) : OnChangedError

    enum class SiteOptionsErrorType {
        INVALID_PARAMETERS,
        TIMEOUT,
        API_ERROR,
        INVALID_RESPONSE,
        AUTHORIZATION_REQUIRED,
        GENERIC_ERROR;
    }
}

fun BaseNetworkError.toSiteOptionsError(): SiteOptionsError {
    val type = when (type) {
        TIMEOUT -> SiteOptionsErrorType.TIMEOUT
        NO_CONNECTION,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        NETWORK_ERROR -> SiteOptionsErrorType.API_ERROR
        PARSE_ERROR,
        NOT_FOUND,
        CENSORED,
        INVALID_RESPONSE -> SiteOptionsErrorType.INVALID_RESPONSE
        HTTP_AUTH_ERROR,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED -> SiteOptionsErrorType.AUTHORIZATION_REQUIRED
        UNKNOWN,
        null -> SiteOptionsErrorType.GENERIC_ERROR
    }
    return SiteOptionsError(type, message)
}
