package org.wordpress.android.repositories

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.fluxc.persistence.EditorSettingsSqlUtils
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ApiUrlResolver
import uniffi.wp_api.WpApiDetails
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EditorSettingsRepository @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val wpLoginClient: WpLoginClient,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val themeRepository: ThemeRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    private val editorSettingsSqlUtils = EditorSettingsSqlUtils()

    fun hasCachedCapabilities(site: SiteModel): Boolean =
        appPrefsWrapper.hasSiteEditorCapabilities(site)

    /**
     * Returns whether the site is known to support the
     * `wp-block-editor/v1/settings` endpoint, based on
     * cached editor settings or a previously persisted
     * result from [fetchEditorCapabilitiesForSite].
     */
    fun getSupportsEditorSettingsForSite(site: SiteModel): Boolean {
        val hasExisting =
            editorSettingsSqlUtils.getEditorSettingsForSite(site) != null
        val cachedPref =
            appPrefsWrapper.getSiteSupportsEditorSettings(site)
        return hasExisting || cachedPref
    }

    /**
     * Returns whether the site is known to support the
     * `wpcom/v2/editor-assets` endpoint, based on a
     * previously persisted result from
     * [fetchEditorCapabilitiesForSite].
     */
    fun getSupportsEditorAssetsForSite(
        site: SiteModel
    ): Boolean =
        appPrefsWrapper.getSiteSupportsEditorAssets(site)

    /**
     * Returns whether the site's active theme is a block
     * theme, based on a previously persisted result from
     * [fetchEditorCapabilitiesForSite].
     */
    fun getThemeSupportsBlockStyles(
        site: SiteModel
    ): Boolean =
        appPrefsWrapper.getSiteThemeIsBlockTheme(site)

    /**
     * Queries the site's REST API to check whether the
     * `wp-block-editor/v1/settings` and
     * `wpcom/v2/editor-assets` routes are available,
     * and fetches the current theme to determine if it
     * is a block theme. All results are persisted so
     * that [getSupportsEditorSettingsForSite],
     * [getSupportsEditorAssetsForSite], and
     * [getThemeSupportsBlockStyles] return them
     * synchronously on future calls.
     *
     * @return `true` when both checks complete without
     *   transport-level failures.
     */
    suspend fun fetchEditorCapabilitiesForSite(
        site: SiteModel
    ): Boolean = withContext(ioDispatcher) {
        val results = awaitAll(
            async { fetchRouteSupport(site) },
            async { fetchThemeBlockStyleSupport(site) }
        )
        results.all { it }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchRouteSupport(
        site: SiteModel
    ): Boolean = try {
        // For Atomic sites the editor fetches `wp-block-editor/v1/settings`
        // from the direct host — proxy and direct host can advertise
        // different route lists, so detection has to probe the direct host
        // too. See #22879.
        if (site.isWPComAtomic) {
            fetchRouteSupportViaDirectHostDiscovery(site)
        } else {
            fetchRouteSupportViaConfiguredClient(site)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.e(
            T.EDITOR,
            "Failed to fetch route support" +
                " for site=${site.name}",
            e
        )
        false
    }

    private suspend fun fetchRouteSupportViaConfiguredClient(
        site: SiteModel
    ): Boolean {
        val client = wpApiClientProvider.getWpApiClient(site)
        val resolver = wpApiClientProvider.getApiUrlResolver(site)
        val response = client.request { it.apiRoot().get() }
        return if (response is WpRequestResult.Success) {
            persistRouteSupport(site, response.response.data, resolver)
            true
        } else {
            false
        }
    }

    /**
     * On WP.com Atomic sites the editor fetches `wp-block-editor/v1/settings`
     * from the direct host — not the WP.com proxy — so detection has to
     * match. Run REST API autodiscovery on the site URL so we don't have to
     * assume the API lives at `/wp-json` (custom permalink structures or
     * REST API paths would break that assumption), then use the routes list
     * returned by discovery directly — no second request needed.
     */
    private suspend fun fetchRouteSupportViaDirectHostDiscovery(
        site: SiteModel
    ): Boolean {
        val discovery = wpLoginClient.apiDiscovery(site.url)
        if (discovery !is ApiDiscoveryResult.Success) {
            AppLog.w(
                T.EDITOR,
                "Direct-host API discovery failed for" +
                    " site=${site.name}: ${discovery::class.simpleName}"
            )
            return false
        }
        val resolver = wpApiClientProvider.urlResolverFor(
            discovery.success.apiRootUrl
        )
        persistRouteSupport(site, discovery.success.apiDetails, resolver)
        return true
    }

    private fun persistRouteSupport(
        site: SiteModel,
        data: WpApiDetails,
        resolver: ApiUrlResolver,
    ) {
        appPrefsWrapper.setSiteSupportsEditorSettings(
            site,
            data.hasRouteForEndpoint(
                resolver,
                "/wp-block-editor/v1",
                "settings"
            )
        )
        appPrefsWrapper.setSiteSupportsEditorAssets(
            site,
            data.hasRouteForEndpoint(
                resolver,
                "/wpcom/v2",
                "editor-assets"
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchThemeBlockStyleSupport(
        site: SiteModel
    ): Boolean = try {
        val theme =
            themeRepository.fetchCurrentTheme(site)
        if (theme != null) {
            AppLog.d(
                T.EDITOR,
                "EditorSettingsRepository: theme fetched for " +
                    "site=${site.name}, " +
                    "themeName=${theme.name}, " +
                    "isBlockTheme=${theme.isBlockTheme}"
            )
            appPrefsWrapper.setSiteThemeIsBlockTheme(
                site, theme.isBlockTheme
            )
            true
        } else {
            false
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.e(
            T.EDITOR,
            "Failed to fetch theme info" +
                " for site=${site.name}",
            e
        )
        false
    }
}
