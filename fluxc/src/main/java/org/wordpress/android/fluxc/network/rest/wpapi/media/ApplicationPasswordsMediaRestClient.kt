package org.wordpress.android.fluxc.network.rest.wpapi.media

import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.endpoint.WPAPIEndpoint
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCreationResult.Created
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCreationResult.Existing
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCreationResult.Failure
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCreationResult.NotSupported
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsManager
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsMediaRestClient @Inject constructor(
    dispatcher: Dispatcher,
    coroutineEngine: CoroutineEngine,
    @Named("no-cookies") okHttpClient: OkHttpClient,
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork
) : BaseWPV2MediaRestClient(dispatcher, coroutineEngine, okHttpClient) {
    @Inject internal lateinit var applicationPasswordsManager: ApplicationPasswordsManager

    override fun WPAPIEndpoint.getFullUrl(site: SiteModel): String {
        return (site.wpApiRestUrl ?: site.url.slashJoin("wp-json")).slashJoin(urlV2)
    }

    override suspend fun getAuthorizationHeader(site: SiteModel): String {
        val credentials = when (val result = applicationPasswordsManager.getApplicationCredentials(site)) {
            is Created -> result.credentials
            is Existing -> result.credentials
            // If there is no saved password yet or the creation fails, the request will simply fail with a 401 error
            // This is unlikely to happen though, since media handling happens later in the app
            is Failure -> null
            is NotSupported -> null
        }

        return credentials?.let {
            Credentials.basic(credentials.userName, credentials.password)
        }.orEmpty()
    }

    override suspend fun <T : Any> executeGetGsonRequest(
        site: SiteModel,
        endpoint: WPAPIEndpoint,
        params: Map<String, String>,
        clazz: Class<T>
    ): WPAPIResponse<T> {
        return applicationPasswordsNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint.urlV2,
            clazz = clazz,
            params = params
        )
    }
}
