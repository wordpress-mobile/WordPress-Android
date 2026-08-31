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
import kotlin.coroutines.cancellation.CancellationException

/**
 * Heals [SiteModel.xmlRpcUrl] for true self-hosted sites whose XML-RPC endpoint was never
 * discovered (or was previously gated). The REST counterpart is [SiteApiRestUrlRecoverer]; this
 * follows the same shape so callers never hold a mutated [SiteModel]:
 *
 * - [discoverAndVerifyXmlRpcUrl] discovers the endpoint and confirms it works with an authenticated
 *   call using the site's application-password credentials, returning an [XmlRpcRecovery] that
 *   separates a verified URL from a *definitive* "XML-RPC is off" from an inconclusive failure.
 * - [persistXmlRpcUrl] writes only that one column to the DB row for `localId`.
 *
 * The three-way outcome matters because a missing `xmlRpcUrl` is not evidence that XML-RPC is
 * disabled: discovery can also fail transiently (e.g. a 429 rate-limit). Only a definitive negative
 * may surface the "XML-RPC Disabled" card, or throttled sites get a false warning.
 */
@Singleton
class SiteXmlRpcUrlRecoverer @Inject constructor(
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteXMLRPCClient: SiteXMLRPCClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val appLogWrapper: AppLogWrapper,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    suspend fun discoverAndVerifyXmlRpcUrl(site: SiteModel): XmlRpcRecovery = withContext(bgDispatcher) {
        try {
            val endpoint = selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(site.url)
            val result = siteXMLRPCClient.fetchSites(
                endpoint,
                site.apiRestUsernamePlain,
                site.apiRestPasswordPlain,
            )
            if (result.isError) {
                // Discovery found a working xmlrpc.php, so XML-RPC is *enabled* — the credentials or the
                // transport are what failed. Don't persist, and don't claim XML-RPC is off.
                appLogWrapper.w(AppLog.T.API, "XML-RPC verification failed for ${site.url}")
                XmlRpcRecovery.Inconclusive
            } else {
                XmlRpcRecovery.Recovered(endpoint)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SelfHostedEndpointFinder.DiscoveryException) {
            if (e.discoveryError.indicatesXmlRpcUnavailable()) {
                appLogWrapper.w(AppLog.T.API, "XML-RPC unavailable for ${site.url} (${e.discoveryError})")
                XmlRpcRecovery.Unavailable
            } else {
                appLogWrapper.w(
                    AppLog.T.API,
                    "XML-RPC discovery inconclusive for ${site.url} (${e.discoveryError})"
                )
                XmlRpcRecovery.Inconclusive
            }
        } catch (e: Exception) {
            // Best-effort recovery must never let an unexpected throw escape and cancel the
            // provisioning pipeline (mirrors SiteApiRestUrlRecoverer). An unexpected throw says nothing
            // about the endpoint, so it's inconclusive rather than a negative.
            appLogWrapper.e(
                AppLog.T.API,
                "XML-RPC discovery threw for ${site.url}: ${e::class.simpleName}: ${e.message}"
            )
            XmlRpcRecovery.Inconclusive
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

    // A failed discovery is only a genuine "disabled" signal when it reached a definitive conclusion.
    // Transient errors (RATE_LIMITED, GENERIC_ERROR) and unrelated conditions (auth/SSL/invalid URL)
    // must not surface the warning, to avoid false positives on throttled sites.
    private fun SelfHostedEndpointFinder.DiscoveryError.indicatesXmlRpcUnavailable(): Boolean =
        this == SelfHostedEndpointFinder.DiscoveryError.NO_SITE_ERROR ||
            this == SelfHostedEndpointFinder.DiscoveryError.MISSING_XMLRPC_METHOD ||
            this == SelfHostedEndpointFinder.DiscoveryError.XMLRPC_BLOCKED ||
            this == SelfHostedEndpointFinder.DiscoveryError.XMLRPC_FORBIDDEN
}

/** The outcome of an XML-RPC endpoint recovery attempt. See [SiteXmlRpcUrlRecoverer]. */
sealed interface XmlRpcRecovery {
    /** Discovered and authenticated against [endpoint] — safe to persist. */
    data class Recovered(val endpoint: String) : XmlRpcRecovery

    /** Discovery reached a definitive negative: the site really has XML-RPC off. */
    data object Unavailable : XmlRpcRecovery

    /** The attempt failed without settling the question (transient error, bad credentials,
     *  unexpected throw). Claim nothing; the next run retries. */
    data object Inconclusive : XmlRpcRecovery
}
