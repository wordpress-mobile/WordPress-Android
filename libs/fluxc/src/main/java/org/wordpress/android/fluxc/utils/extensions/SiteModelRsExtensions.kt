package org.wordpress.android.fluxc.utils.extensions

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.rs.RsSite
import org.wordpress.android.networking.rs.WpApiClientProvider
import org.wordpress.android.networking.rs.shouldUseWpComProxy
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.ApiUrlResolver

/**
 * Canonical conversion from fluxc's [SiteModel] to the RS networking
 * layer's fluxc-free [RsSite] descriptor. Every other bridge in this
 * file routes through this one.
 */
fun SiteModel.toRsSite(): RsSite = RsSite(
    localId = id,
    siteId = siteId,
    url = url.orEmpty(),
    wpApiRestUrl = wpApiRestUrl,
    isWPCom = isWPCom,
    isWPComAtomic = isWPComAtomic,
    isJetpackConnected = isJetpackConnected,
    isUsingWpComRestApi = origin == SiteModel.ORIGIN_WPCOM_REST,
    apiRestUsernamePlain = apiRestUsernamePlain.orEmpty(),
    apiRestPasswordPlain = apiRestPasswordPlain.orEmpty(),
    username = username.orEmpty(),
    password = password.orEmpty(),
)

fun SiteModel.shouldUseWpComProxy(): Boolean = toRsSite().shouldUseWpComProxy()

fun WpApiClientProvider.getWpApiClient(
    site: SiteModel,
    uploadListener: WpRequestExecutor.UploadListener? = null
): WpApiClient = getWpApiClient(site.toRsSite(), uploadListener)

fun WpApiClientProvider.getWpApiClientCookiesNonceAuthentication(
    site: SiteModel
): WpApiClient = getWpApiClientCookiesNonceAuthentication(site.toRsSite())

fun WpApiClientProvider.getApiUrlResolver(site: SiteModel): ApiUrlResolver =
    getApiUrlResolver(site.toRsSite())

fun WpApiClientProvider.getApiRootUrlFrom(site: SiteModel): String =
    getApiRootUrlFrom(site.toRsSite())
