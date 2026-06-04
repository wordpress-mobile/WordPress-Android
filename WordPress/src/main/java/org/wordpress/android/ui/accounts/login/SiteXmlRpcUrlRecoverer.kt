package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Heals [SiteModel.xmlRpcUrl] for true self-hosted sites whose XML-RPC endpoint was never
 * discovered (or was previously gated). The REST counterpart is [SiteApiRestUrlRecoverer]; this
 * follows the same shape so callers never hold a mutated [SiteModel]:
 *
 * - [discoverAndVerifyXmlRpcUrl] discovers the endpoint and confirms it works with an authenticated
 *   call, returning the verified URL (or `null` if discovery/verification fails).
 * - [persistXmlRpcUrl] writes only that one column to the DB row for `localId`.
 */
@Singleton
class SiteXmlRpcUrlRecoverer @Inject constructor(
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteXMLRPCClient: SiteXMLRPCClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val appLogWrapper: AppLogWrapper,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    @Suppress("SwallowedException")
    suspend fun discoverAndVerifyXmlRpcUrl(site: SiteModel): String? = withContext(bgDispatcher) {
        try {
            val endpoint = selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(site.url)
            val result = siteXMLRPCClient.fetchSites(
                endpoint,
                site.apiRestUsernamePlain,
                site.apiRestPasswordPlain,
            )
            if (result.isError) {
                appLogWrapper.w(AppLog.T.API, "XML-RPC verification failed for ${site.url}")
                null
            } else {
                endpoint
            }
        } catch (e: SelfHostedEndpointFinder.DiscoveryException) {
            appLogWrapper.w(AppLog.T.API, "XML-RPC discovery failed for ${site.url}")
            null
        }
    }

    suspend fun persistXmlRpcUrl(localId: Int, xmlRpcUrl: String): Boolean = withContext(bgDispatcher) {
        val rowsUpdated = siteSqlUtils.updateXmlRpcUrl(localId, xmlRpcUrl)
        if (rowsUpdated == 0) {
            appLogWrapper.w(AppLog.T.API, "Cannot persist xmlRpcUrl: no site with localId=$localId")
            false
        } else {
            appLogWrapper.d(AppLog.T.API, "Persisted xmlRpcUrl=$xmlRpcUrl for localId=$localId")
            true
        }
    }
}
