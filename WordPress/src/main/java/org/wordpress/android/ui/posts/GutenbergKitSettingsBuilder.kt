package org.wordpress.android.ui.posts

import android.util.Base64
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
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
    ): EditorConfiguration {
        val applicationPassword = site.apiRestPasswordPlain
        val shouldUseWPComRestApi =
            applicationPassword.isNullOrEmpty() && site.isUsingWpComRestApi

        val siteApiRoot = if (shouldUseWPComRestApi) {
            WPCOM_API_ROOT
        } else {
            site.wpApiRestUrl ?: "${site.url}/wp-json/"
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

        val postType = if (post?.isPage == true) PostTypeDetails.page else PostTypeDetails.post

        val cachedHosts = buildCachedHosts(site.url)
        val thirdPartyBlocks = editorCapabilityResolver.resolveThirdPartyBlocks(site)
        val editorAssetsEndpoint = if (thirdPartyBlocks.isAvailable) {
            buildEditorAssetsEndpoint(siteApiRoot, siteApiNamespace)
        } else {
            null
        }

        return EditorConfiguration.builder(
            siteURL = site.url,
            siteApiRoot = siteApiRoot,
            postType = postType
        ).apply {
            setTitle(post?.title ?: "")
            setContent(post?.content ?: "")
            setPostId(
                if (post?.isLocalDraft == true) null
                else post?.remotePostId?.toUInt()
            )
            setPostStatus(post?.status ?: "draft")
            setAuthHeader(authHeader)
            setSiteApiNamespace(siteApiNamespace)
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

    companion object {
        private const val AUTH_BEARER_PREFIX = "Bearer "
        private const val AUTH_BASIC_PREFIX = "Basic "
        private const val WPCOM_API_ROOT =
            "https://public-api.wordpress.com/"
    }
}
