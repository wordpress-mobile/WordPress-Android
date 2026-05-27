package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.DiscoverSuccessWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Heals [SiteModel.wpApiRestUrl] when it's missing — WP.com `/me/sites` omits the field, and
 * headless application-password mint runs through the Jetpack tunnel without doing discovery.
 *
 * - [discoverApiRootUrl] runs REST API autodiscovery and returns the discovered root URL.
 * - [persistApiRootUrl] writes only that one column to the DB row for `localId`.
 *
 * Callers handle the "is it missing?" check and the in-memory assignment themselves so the
 * mutation stays visible at the call site.
 */
@Singleton
class SiteApiRestUrlRecoverer @Inject constructor(
    private val wpLoginClient: WpLoginClient,
    private val discoverSuccessWrapper: DiscoverSuccessWrapper,
    private val siteSqlUtils: SiteSqlUtils,
    private val appLogWrapper: AppLogWrapper,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun discoverApiRootUrl(siteUrl: String): String? = withContext(bgDispatcher) {
        try {
            when (val result = wpLoginClient.apiDiscovery(siteUrl)) {
                is ApiDiscoveryResult.Success -> {
                    val apiRootUrl = discoverSuccessWrapper.getApiRootUrl(result)
                    if (apiRootUrl.isBlank()) null else apiRootUrl
                }
                else -> {
                    appLogWrapper.w(AppLog.T.API, "API discovery failed for $siteUrl")
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            appLogWrapper.e(
                AppLog.T.API,
                "API discovery threw for $siteUrl: ${e::class.simpleName}: ${e.message}"
            )
            null
        }
    }

    suspend fun persistApiRootUrl(localId: Int, apiRootUrl: String): Boolean = withContext(bgDispatcher) {
        val rowsUpdated = siteSqlUtils.updateWpApiRestUrl(localId, apiRootUrl)
        if (rowsUpdated == 0) {
            appLogWrapper.w(AppLog.T.API, "Cannot persist wpApiRestUrl: no site with localId=$localId")
            false
        } else {
            appLogWrapper.d(AppLog.T.API, "Persisted wpApiRestUrl=$apiRootUrl for localId=$localId")
            true
        }
    }
}
