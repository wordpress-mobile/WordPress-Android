package org.wordpress.android.repositories

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.fluxc.persistence.EditorSettingsSqlUtils
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EditorSettingsRepository @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val editorAuthHeaderBuilder: EditorAuthHeaderBuilder,
    @Named(OkHttpClientQualifiers.REGULAR) private val okHttpClient: OkHttpClient,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    private val editorSettingsSqlUtils = EditorSettingsSqlUtils()

    fun hasCachedCapabilities(site: SiteModel): Boolean =
        appPrefsWrapper.hasSiteEditorCapabilities(site)

    /**
     * Returns whether the site is known to support the
     * `wp-block-editor/v1/settings` endpoint.
     *
     * Once [fetchEditorCapabilitiesForSite] has probed the
     * endpoint and persisted an answer, that result is
     * authoritative — it correctly reflects sites where the
     * endpoint was once available but no longer is.
     *
     * Before the probe has answered (e.g. on first launch),
     * a non-null row in the legacy [EditorSettingsSqlUtils]
     * cache stands in as a fast-path "we successfully fetched
     * settings here before."
     */
    fun getSupportsEditorSettingsForSite(site: SiteModel): Boolean =
        if (appPrefsWrapper.hasSiteEditorCapabilities(site)) {
            appPrefsWrapper.getSiteSupportsEditorSettings(site)
        } else {
            editorSettingsSqlUtils.getEditorSettingsForSite(site) != null
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
     * Probes the editor endpoints the GutenbergKit editor will hit
     * (`wp-block-editor/v1/settings` and `wpcom/v2/editor-assets`)
     * and fetches the current theme to determine if it is a block
     * theme. All results are persisted so that
     * [getSupportsEditorSettingsForSite],
     * [getSupportsEditorAssetsForSite], and
     * [getThemeSupportsBlockStyles] return them synchronously on
     * future calls.
     *
     * Direct endpoint probing — rather than reading the API root's
     * route advertisement — is required because the WP.com proxy
     * advertises editor routes (e.g.
     * `/wp-block-editor/v1/sites/<host>/settings`) even when the
     * underlying site has not registered them and the actual endpoint
     * returns 404. The probe also routes Atomic sites' settings probe
     * to the direct host (see the MUST-NOT-REMOVE block below and
     * #22879/#22883).
     *
     * @return `true` when every probe completes with a definitive
     *   answer (2xx/401/403 → Supported; 404 → Unsupported; theme
     *   fetched). `false` if any check failed with a transport error,
     *   so callers know to retry later.
     */
    suspend fun fetchEditorCapabilitiesForSite(
        site: SiteModel
    ): Boolean = withContext(ioDispatcher) {
        val results = awaitAll(
            async {
                probeAndPersist(
                    site,
                    namespace = "wp-block-editor/v1",
                    endpoint = "settings",
                    // MUST NOT REMOVE — see #22879 / #22883.
                    //
                    // GutenbergKit's `wp-block-editor/v1/settings` fetch goes
                    // to the DIRECT host (it bypasses the configured
                    // `siteApiRoot` for this one endpoint). On Atomic the
                    // WP.com proxy can advertise/serve this route while the
                    // direct host 404s it — probing the proxy here would
                    // false-positive and the editor would then 404 on the
                    // direct host (the original bug). Capability detection
                    // for this endpoint MUST follow the editor to the direct
                    // host on Atomic. Do not unify with the editor-assets
                    // probe below — editor-assets does go through the
                    // configured root, which is the proxy for WPCom-routed
                    // sites.
                    forceDirectHost = site.isWPComAtomic,
                    persist = appPrefsWrapper::setSiteSupportsEditorSettings,
                )
            },
            async {
                probeAndPersist(
                    site,
                    namespace = "wpcom/v2",
                    endpoint = "editor-assets",
                    forceDirectHost = false,
                    persist = appPrefsWrapper::setSiteSupportsEditorAssets,
                )
            },
            async { fetchThemeBlockStyleSupport(site) }
        )
        results.all { it }
    }

    private suspend fun probeAndPersist(
        site: SiteModel,
        namespace: String,
        endpoint: String,
        forceDirectHost: Boolean,
        persist: (SiteModel, Boolean) -> Unit,
    ): Boolean = when (probeEndpoint(site, namespace, endpoint, forceDirectHost)) {
        ProbeResult.Supported -> {
            persist(site, true)
            true
        }
        ProbeResult.Unsupported -> {
            persist(site, false)
            true
        }
        ProbeResult.Inconclusive -> false
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun probeEndpoint(
        site: SiteModel,
        namespace: String,
        endpoint: String,
        forceDirectHost: Boolean,
    ): ProbeResult {
        val url = buildProbeUrl(site, namespace, endpoint, forceDirectHost)
        val authHeader = editorAuthHeaderBuilder.build(
            site, accountStore.accessToken
        ) ?: return ProbeResult.Inconclusive
        val request = Request.Builder()
            .url(url)
            .header(AUTHORIZATION_HEADER, authHeader)
            .get()
            .build()
        return try {
            executeProbe(request).use { response ->
                when {
                    response.isSuccessful -> ProbeResult.Supported
                    // 401/403 mean the route exists but auth was rejected /
                    // insufficient — still "registered." Critical for the
                    // Atomic direct-host probe where the WPCom bearer token
                    // is not necessarily honored by the direct host.
                    response.code == HttpURLConnection.HTTP_UNAUTHORIZED ||
                        response.code == HttpURLConnection.HTTP_FORBIDDEN ->
                        ProbeResult.Supported
                    response.code == HttpURLConnection.HTTP_NOT_FOUND ->
                        ProbeResult.Unsupported
                    else -> ProbeResult.Inconclusive
                }
            }
        } catch (e: IOException) {
            AppLog.e(
                T.EDITOR,
                "Probe failed for $namespace/$endpoint" +
                    " on site=${site.name}",
                e
            )
            ProbeResult.Inconclusive
        } catch (e: IllegalArgumentException) {
            AppLog.e(
                T.EDITOR,
                "Probe URL invalid for $namespace/$endpoint" +
                    " on site=${site.name}: $url",
                e
            )
            ProbeResult.Inconclusive
        }
    }

    /**
     * Wraps OkHttp's async [Call.enqueue] in a suspending function so
     * that coroutine cancellation propagates to the in-flight HTTP
     * call. Without this, a cancelled preload would leave the socket
     * open until OkHttp's read timeout fires (up to ~60s).
     */
    private suspend fun executeProbe(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = okHttpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) cont.resume(response)
                    else response.close()
                }
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            })
        }

    /**
     * Mirrors the URL the editor would hit: WP.com-routed sites go
     * through the proxy with a `/sites/<id>/` prefix; self-hosted
     * sites go to the direct REST root.
     *
     * [forceDirectHost] overrides the proxy routing for endpoints
     * where GutenbergKit bypasses `siteApiRoot` (currently only
     * `wp-block-editor/v1/settings` on Atomic). See the
     * MUST-NOT-REMOVE block in [fetchEditorCapabilitiesForSite].
     */
    private fun buildProbeUrl(
        site: SiteModel,
        namespace: String,
        endpoint: String,
        forceDirectHost: Boolean,
    ): String {
        val applicationPassword = site.apiRestPasswordPlain
        val shouldUseProxy = !forceDirectHost &&
            applicationPassword.isNullOrEmpty() &&
            site.isUsingWpComRestApi
        val ns = namespace.trim('/')
        val ep = endpoint.trim('/')
        return if (shouldUseProxy) {
            "$WPCOM_API_ROOT$ns/sites/${site.siteId}/$ep"
        } else {
            val apiRoot = site.wpApiRestUrl
                ?.takeIf { it.isNotEmpty() }
                ?: "${site.url}/wp-json/"
            val withSlash =
                if (apiRoot.endsWith('/')) apiRoot else "$apiRoot/"
            "$withSlash$ns/$ep"
        }
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

    private enum class ProbeResult { Supported, Unsupported, Inconclusive }

    companion object {
        private const val WPCOM_API_ROOT =
            "https://public-api.wordpress.com/"
        private const val AUTHORIZATION_HEADER = "Authorization"
    }
}
