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
 * Discovers and populates [SiteModel.wpApiRestUrl] when it's missing, healing sites that
 * landed in the DB without one (WP.com `/me/sites` omits the field; headless application-
 * password mint goes through the Jetpack tunnel without running discovery).
 *
 * - [recoverAndPersistIfMissing] writes to the DB. Used from the auth flow.
 * - [discoverInMemoryIfMissing] sets the in-memory model only — for short-lived consumers
 *   (editor preloader) that just need the URL for one call.
 */
@Singleton
class SiteApiRestUrlRecoverer @Inject constructor(
    private val wpLoginClient: WpLoginClient,
    private val discoverSuccessWrapper: DiscoverSuccessWrapper,
    private val siteSqlUtils: SiteSqlUtils,
    private val appLogWrapper: AppLogWrapper,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    suspend fun recoverAndPersistIfMissing(site: SiteModel) {
        if (!site.wpApiRestUrl.isNullOrEmpty()) return
        withContext(bgDispatcher) {
            val apiRootUrl = runDiscovery(site) ?: return@withContext
            site.wpApiRestUrl = apiRootUrl
            persist(site.id, apiRootUrl)
        }
    }

    suspend fun discoverInMemoryIfMissing(site: SiteModel) {
        if (!site.wpApiRestUrl.isNullOrEmpty()) return
        withContext(bgDispatcher) {
            val apiRootUrl = runDiscovery(site) ?: return@withContext
            site.wpApiRestUrl = apiRootUrl
            appLogWrapper.d(
                AppLog.T.API,
                "Discovered wpApiRestUrl=$apiRootUrl for ${site.url} (in-memory only)"
            )
        }
    }

    // Re-reads the DB row by local ID and writes only [SiteModel.wpApiRestUrl]. This preserves
    // anything that other code paths (e.g. an UPDATE_APPLICATION_PASSWORD that ran between site
    // load and here) wrote since the in-memory model was last loaded. Writing the in-memory site
    // directly is *unsafe* — it would clobber concurrent updates to other fields.
    @Suppress("SwallowedException")
    private fun persist(localId: Int, apiRootUrl: String) {
        val siteFromDB = siteSqlUtils.getSitesWithLocalId(localId).firstOrNull() ?: run {
            appLogWrapper.w(AppLog.T.API, "Cannot persist wpApiRestUrl: no site with localId=$localId")
            return
        }
        siteFromDB.wpApiRestUrl = apiRootUrl
        try {
            siteSqlUtils.insertOrUpdateSite(siteFromDB)
            appLogWrapper.d(
                AppLog.T.API,
                "Recovered wpApiRestUrl=$apiRootUrl for ${siteFromDB.url} (persisted)"
            )
        } catch (e: SiteSqlUtils.DuplicateSiteException) {
            appLogWrapper.e(
                AppLog.T.API,
                "DuplicateSiteException persisting wpApiRestUrl=$apiRootUrl for ${siteFromDB.url}"
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runDiscovery(site: SiteModel): String? = try {
        when (val result = wpLoginClient.apiDiscovery(site.url)) {
            is ApiDiscoveryResult.Success -> {
                val apiRootUrl = discoverSuccessWrapper.getApiRootUrl(result)
                if (apiRootUrl.isBlank()) null else apiRootUrl
            }
            else -> {
                appLogWrapper.w(
                    AppLog.T.API,
                    "API discovery failed for ${site.url}"
                )
                null
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        appLogWrapper.e(
            AppLog.T.API,
            "API discovery threw for ${site.url}: ${e::class.simpleName}: ${e.message}"
        )
        null
    }
}
