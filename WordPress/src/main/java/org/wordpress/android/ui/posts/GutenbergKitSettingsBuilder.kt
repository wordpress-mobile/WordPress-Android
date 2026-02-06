package org.wordpress.android.ui.posts

import android.util.Base64
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.gutenberg.model.EditorConfiguration
import java.net.URI

object GutenbergKitSettingsBuilder {
    private const val AUTH_BEARER_PREFIX = "Bearer "
    private const val AUTH_BASIC_PREFIX = "Basic "
    private const val WPCOM_API_ROOT = "https://public-api.wordpress.com/"

    fun buildPostConfiguration(
        site: SiteModel,
        post: PostImmutableModel? = null,
        accessToken: String?
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

        val postType = if (post?.isPage == true) "page" else "post"

        val siteHost = extractHost(site.url)
        val cachedHosts = if (!siteHost.isNullOrEmpty()) {
            setOf("s0.wp.com", siteHost)
        } else {
            setOf("s0.wp.com")
        }

        val firstNamespace = siteApiNamespace.firstOrNull() ?: ""
        val editorAssetsEndpoint = if (
            firstNamespace.isNotEmpty() && siteApiRoot.isNotEmpty()
        ) {
            "${siteApiRoot}wpcom/v2/${firstNamespace}editor-assets"
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
            setPostId(post?.remotePostId?.toInt())
            setPostStatus(post?.status ?: "draft")
            setAuthHeader(authHeader)
            setSiteApiNamespace(siteApiNamespace)
            setNamespaceExcludedPaths(
                arrayOf(
                    "/wpcom/v2/following/recommendations",
                    "/wpcom/v2/following/mine"
                )
            )
            setThemeStyles(false)
            setPlugins(false)
            setLocale("en")
            setCookies(emptyMap())
            setEnableAssetCaching(true)
            setCachedAssetHosts(cachedHosts)
            setEditorAssetsEndpoint(editorAssetsEndpoint)
            setEnableNetworkLogging(false)
        }.build()
    }

    fun buildAuthHeader(
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

    fun shouldUsePlugins(
        isFeatureEnabled: Boolean,
        isWPComSite: Boolean,
        isJetpackConnected: Boolean,
        applicationPassword: String?
    ): Boolean {
        return isFeatureEnabled &&
            (isWPComSite ||
                (isJetpackConnected && !applicationPassword.isNullOrEmpty()))
    }

    internal fun buildSiteApiNamespace(
        shouldUseWPComRestApi: Boolean,
        siteId: Long,
        siteUrl: String
    ): Array<String> {
        if (!shouldUseWPComRestApi) return arrayOf()
        val host = extractHost(siteUrl) ?: return arrayOf("sites/$siteId/")
        return arrayOf("sites/$siteId/", "sites/$host/")
    }

    internal fun extractHost(url: String): String? {
        return try {
            URI(url).host
        } catch (_: Exception) {
            null
        }
    }
}
