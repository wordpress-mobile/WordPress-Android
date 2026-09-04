package org.wordpress.android.ui.posts

import android.util.Base64
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WPComApiProxy
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PerAppLocaleManager
import org.wordpress.gutenberg.model.EditorConfiguration
import org.wordpress.gutenberg.model.PostTypeDetails
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GutenbergKitSettingsBuilder @Inject constructor(
    private val editorCapabilityResolver: EditorCapabilityResolver,
    private val perAppLocaleManager: PerAppLocaleManager,
) {
    fun buildPostConfiguration(
        site: SiteModel,
        accessToken: String?,
        cookies: Map<String, String>,
        isNetworkLoggingEnabled: Boolean,
        post: PostImmutableModel? = null,
        source: ConfigSource = ConfigSource.EDITOR,
    ): EditorConfiguration {
        val transport = resolveApiTransport(site, accessToken, source)
        val postType = if (post?.isPage == true) PostTypeDetails.page else PostTypeDetails.post

        val cachedHosts = buildCachedHosts(site.url)
        val thirdPartyBlocks = editorCapabilityResolver.resolveThirdPartyBlocks(site)
        val editorAssetsEndpoint = if (thirdPartyBlocks.isAvailable) {
            buildEditorAssetsEndpoint(transport.siteApiRoot, transport.siteApiNamespace)
        } else {
            null
        }

        return EditorConfiguration.builder(
            siteURL = site.url,
            siteApiRoot = transport.siteApiRoot,
            postType = postType
        ).apply {
            setTitle(post?.title ?: "")
            setContent(post?.content ?: "")
            setPostId(
                if (post?.isLocalDraft == true) null
                else post?.remotePostId?.toUInt()
            )
            setPostStatus(post?.status ?: "draft")
            setAuthHeader(transport.authHeader)
            setSiteApiNamespace(transport.siteApiNamespace)
            setNamespaceExcludedPaths(
                arrayOf(
                    "/wpcom/v2/following/recommendations",
                    "/wpcom/v2/following/mine"
                )
            )
            setThemeStyles(
                editorCapabilityResolver.resolveThemeStyles(site).shouldApplyInEditor
            )
            setPlugins(thirdPartyBlocks.shouldApplyInEditor)
            setLocale(perAppLocaleManager.getCurrentLocale())
            setCookies(cookies)
            setEnableAssetCaching(true)
            setCachedAssetHosts(cachedHosts)
            setEditorAssetsEndpoint(editorAssetsEndpoint)
            setEnableNetworkLogging(isNetworkLoggingEnabled)
            setEnableNativeBlockInserter(true)
        }.build()
    }

    /**
     * How the editor reaches this site's REST API. The three values are derived from one another
     * and only make sense together: a WP.com root needs [siteApiNamespace] to carry
     * `sites/<id>/`, a direct-host root needs it empty, and [authHeader] has to match the host
     * being addressed. Resolving them in one place is what keeps them from drifting apart.
     */
    private class ApiTransport(
        val siteApiRoot: String,
        val siteApiNamespace: Array<String>,
        val authHeader: String,
    )

    private fun resolveApiTransport(
        site: SiteModel,
        accessToken: String?,
        source: ConfigSource
    ): ApiTransport {
        val applicationPassword = site.apiRestPasswordPlain
        val shouldUseWPComRestApi =
            applicationPassword.isNullOrEmpty() && site.isUsingWpComRestApi

        val siteApiRoot = if (shouldUseWPComRestApi) {
            WPComApiProxy.ROOT
        } else {
            resolveDirectHostApiRoot(site)
        }

        val authHeader = buildAuthHeader(
            shouldUseWPComRestApi = shouldUseWPComRestApi,
            accessToken = accessToken,
            username = site.apiRestUsernamePlain,
            password = applicationPassword
        ) ?: ""

        val siteApiNamespace = buildSiteApiNamespace(
            shouldUseWPComRestApi, site.siteId, site.url
        )

        logSiteApiRouting(site, siteApiRoot, shouldUseWPComRestApi, source)

        return ApiTransport(siteApiRoot, siteApiNamespace, authHeader)
    }

    /**
     * The site's own REST root, for the branch that sends unnamespaced paths straight to the host.
     *
     * A [WPComApiProxy] root cannot serve that purpose: it already embeds its namespace, so
     * GutenbergKit would append an already-namespaced path to it and every request would 404 with
     * `rest_no_route`. `SiteSqlUtils.updateWpApiRestUrl` refuses to store one and the migration in
     * `WellSqlConfig` clears those older installs already hold, so this is a backstop for a row
     * that predates both.
     */
    internal fun resolveDirectHostApiRoot(site: SiteModel): String {
        val fallback = "${site.url}/wp-json/"
        val stored = site.wpApiRestUrl?.takeIf { it.isNotEmpty() }
        if (WPComApiProxy.isProxyRoot(stored)) {
            AppLog.e(
                AppLog.T.EDITOR,
                "Ignoring WP.com proxy root stored for ${site.url}: it already embeds its" +
                    " namespace, so it cannot be used as a direct-host root ($stored)"
            )
            return fallback
        }
        return stored?.withTrailingSlash() ?: fallback
    }

    /**
     * GutenbergKit's JS strips a path's leading slash and concatenates it onto the root
     * (`api-fetch`'s `createRootURLMiddleware`), and [buildEditorAssetsEndpoint] concatenates with
     * no separator either, so a root without a trailing slash yields `…/wp-jsonwp/v2/types`.
     * `WpApiClientProvider` builds slash-less roots (`"${url}/wp-json"`) that reach the stored
     * column through the application-password login, so normalize rather than trusting the writer.
     *
     * Roots carrying a query string are the plain-permalink `?rest_route=` form, which `api-fetch`
     * handles by switching the path's `?` to `&`. Appending a slash there would corrupt them.
     */
    private fun String.withTrailingSlash(): String =
        if (endsWith("/") || contains("?")) this else "$this/"

    /**
     * Records how the editor resolved its REST root, alongside the site classification it was
     * derived from, so an editor that fails to load can be traced without a debugger.
     *
     * [source] matters because the two entry points read the site differently — the preloader
     * re-reads it from the store, the editor uses the copy serialized into its intent — so lines
     * that disagree for one launch point at a stale in-memory model rather than a stale row.
     */
    private fun logSiteApiRouting(
        site: SiteModel,
        siteApiRoot: String,
        shouldUseWPComRestApi: Boolean,
        source: ConfigSource
    ) {
        AppLog.d(
            AppLog.T.EDITOR,
            "Editor API routing for ${site.url}: source=$source" +
                " wpcomRest=$shouldUseWPComRestApi root=$siteApiRoot" +
                " isWPCom=${site.isWPCom} isAtomic=${site.isWPComAtomic}" +
                " isSimple=${site.isWPComSimpleSite} isPrivate=${site.isPrivate}" +
                " comingSoon=${site.isComingSoon} origin=${site.origin}" +
                " hasAppPassword=${!site.apiRestPasswordPlain.isNullOrEmpty()}"
        )
    }

    internal fun buildAuthHeader(
        shouldUseWPComRestApi: Boolean,
        accessToken: String?,
        username: String?,
        password: String?
    ): String? {
        return if (shouldUseWPComRestApi) {
            if (!accessToken.isNullOrEmpty()) {
                "$AUTH_BEARER_PREFIX$accessToken"
            } else {
                AppLog.w(
                    AppLog.T.EDITOR,
                    "Missing access token for WP.com REST API authentication"
                )
                null
            }
        } else {
            if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                try {
                    val credentials = "$username:$password"
                    val encodedCredentials = Base64.encodeToString(
                        credentials.toByteArray(Charsets.UTF_8),
                        Base64.NO_WRAP
                    )
                    "$AUTH_BASIC_PREFIX$encodedCredentials"
                } catch (e: IllegalArgumentException) {
                    AppLog.e(
                        AppLog.T.EDITOR,
                        "Failed to encode Basic auth credentials",
                        e
                    )
                    null
                }
            } else {
                AppLog.w(
                    AppLog.T.EDITOR,
                    "Incomplete credentials for Basic authentication"
                )
                null
            }
        }
    }

    internal fun buildSiteApiNamespace(
        shouldUseWPComRestApi: Boolean,
        siteId: Long,
        siteUrl: String
    ): Array<String> {
        if (!shouldUseWPComRestApi) return arrayOf()
        val host = extractHost(siteUrl)
        return if (host != null) {
            arrayOf("sites/$siteId/", "sites/$host/")
        } else {
            arrayOf("sites/$siteId/")
        }
    }

    private fun buildCachedHosts(siteUrl: String): Set<String> {
        val siteHost = extractHost(siteUrl)
        return if (!siteHost.isNullOrEmpty()) {
            setOf("s0.wp.com", siteHost)
        } else {
            setOf("s0.wp.com")
        }
    }

    private fun buildEditorAssetsEndpoint(
        siteApiRoot: String,
        siteApiNamespace: Array<String>,
    ): String {
        val firstNamespace = siteApiNamespace.firstOrNull() ?: ""
        return "${siteApiRoot}wpcom/v2/${firstNamespace}editor-assets"
    }

    internal fun extractHost(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val normalized = if ("://" in trimmed) trimmed else "https://$trimmed"
        return try {
            URI(normalized).host?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Which entry point asked for a configuration. [PRELOADER] re-reads the site from the store,
     * [EDITOR] uses the copy serialized into the editor's intent — so a log line naming the source
     * distinguishes a stale row from a stale in-memory model. Diagnostic only.
     */
    enum class ConfigSource { PRELOADER, EDITOR }

    companion object {
        private const val AUTH_BEARER_PREFIX = "Bearer "
        private const val AUTH_BASIC_PREFIX = "Basic "
    }
}

