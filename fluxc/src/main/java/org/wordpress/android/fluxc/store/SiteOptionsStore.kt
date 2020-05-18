package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettingsMapper
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
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SiteOptionsStore
@Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val siteHomepageRestClient: SiteHomepageRestClient,
    private val siteXMLRPCClient: SiteXMLRPCClient,
    private val siteHomepageSettingsMapper: SiteHomepageSettingsMapper
) {
    suspend fun updateHomepage(site: SiteModel, homepageSettings: SiteHomepageSettings): HomepageUpdatedPayload =
            coroutineEngine.withDefaultContext(T.API, this, "Update homepage settings") {
                return@withDefaultContext if (site.isUsingWpComRestApi) {
                    siteHomepageRestClient.updateHomepage(site, homepageSettings)
                } else {
                    suspendCoroutine { continuation ->
                        siteXMLRPCClient.updateSiteHomepage(
                                site,
                                homepageSettings,
                                { updatedSite ->
                                    val payload = siteHomepageSettingsMapper.map(updatedSite)
                                            ?.let { HomepageUpdatedPayload(it) } ?: HomepageUpdatedPayload(
                                            SiteOptionsError(
                                                    GENERIC_ERROR,
                                                    "Site contains unexpected showOnFront value: ${updatedSite.showOnFront}"
                                            )
                                    )
                                    continuation.resume(payload)
                                },
                                { error ->
                                    continuation.resume(HomepageUpdatedPayload(error))
                                }
                        )
                    }
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
