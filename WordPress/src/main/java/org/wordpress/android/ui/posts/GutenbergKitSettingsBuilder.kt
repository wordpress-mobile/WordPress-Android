package org.wordpress.android.ui.posts

import android.util.Base64
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils

object GutenbergKitSettingsBuilder {
    private const val AUTH_BEARER_PREFIX = "Bearer "
    private const val AUTH_BASIC_PREFIX = "Basic "

    data class SiteConfig(
        val url: String,
        val siteId: Long,
        val isWPCom: Boolean,
        val isWPComAtomic: Boolean,
        val isJetpackConnected: Boolean,
        val isUsingWpComRestApi: Boolean,
        val wpApiRestUrl: String?,
        val apiRestUsernamePlain: String?,
        val apiRestPasswordPlain: String?
    )

    data class PostConfig(
        val remotePostId: Long?,
        val isPage: Boolean,
        val title: String?,
        val content: String?
    )

    data class FeatureConfig(
        val isPluginsFeatureEnabled: Boolean,
        val isThemeStylesFeatureEnabled: Boolean
    )

    data class AppConfig(
        val accessToken: String?,
        val locale: String,
        val cookies: Any?
    )

    /**
     * Builds the settings configuration for GutenbergKit editor.
     *
     * This method determines the appropriate authentication method based on site type:
     * - WP.com sites use Bearer token authentication with the public API
     * - Jetpack/self-hosted sites with application passwords use Basic authentication
     * - Falls back to WP.com REST API when no application password is available
     */
    fun buildSettings(
        siteConfig: SiteConfig,
        postConfig: PostConfig,
        appConfig: AppConfig,
        featureConfig: FeatureConfig
    ): MutableMap<String, Any?> {
        val applicationPassword = siteConfig.apiRestPasswordPlain
        val shouldUseWPComRestApi = applicationPassword.isNullOrEmpty() && siteConfig.isUsingWpComRestApi

        val siteApiRoot = if (shouldUseWPComRestApi) "https://public-api.wordpress.com/"
        else siteConfig.wpApiRestUrl ?: "${siteConfig.url}/wp-json/"

        val authHeader = buildAuthHeader(
            shouldUseWPComRestApi = shouldUseWPComRestApi,
            accessToken = appConfig.accessToken,
            username = siteConfig.apiRestUsernamePlain,
            password = applicationPassword
        )

        val siteApiNamespace = if (shouldUseWPComRestApi)
            arrayOf("sites/${siteConfig.siteId}/", "sites/${UrlUtils.removeScheme(siteConfig.url)}/")
        else arrayOf()

        val wpcomLocaleSlug = appConfig.locale.replace("_", "-").lowercase()

        return mutableMapOf(
            "postId" to postConfig.remotePostId?.toInt(),
            "postType" to if (postConfig.isPage) "page" else "post",
            "postTitle" to postConfig.title,
            "postContent" to postConfig.content,
            "siteURL" to siteConfig.url,
            "siteApiRoot" to siteApiRoot,
            "namespaceExcludedPaths" to arrayOf("/wpcom/v2/following/recommendations", "/wpcom/v2/following/mine"),
            "authHeader" to authHeader,
            "siteApiNamespace" to siteApiNamespace,
            "themeStyles" to featureConfig.isThemeStylesFeatureEnabled,
            "plugins" to shouldUsePlugins(
                isFeatureEnabled = featureConfig.isPluginsFeatureEnabled,
                isWPComSite = siteConfig.isWPCom,
                isJetpackConnected = siteConfig.isJetpackConnected,
                applicationPassword = applicationPassword
            ),
            "locale" to wpcomLocaleSlug,
            "cookies" to appConfig.cookies
        )
    }

    /**
     * Builds the authentication header based on the authentication method.
     *
     * @param shouldUseWPComRestApi True if using WP.com REST API (Bearer auth)
     * @param accessToken The OAuth2 access token for WP.com authentication
     * @param username The username for Basic auth (application passwords)
     * @param password The password for Basic auth (application passwords)
     * @return The formatted authentication header string, or null if credentials are invalid
     */
    private fun buildAuthHeader(
        shouldUseWPComRestApi: Boolean,
        accessToken: String?,
        username: String?,
        password: String?
    ): String? {
        return if (shouldUseWPComRestApi) {
            if (!accessToken.isNullOrEmpty()) {
                "$AUTH_BEARER_PREFIX$accessToken"
            } else {
                AppLog.w(AppLog.T.EDITOR, "Missing access token for WP.com REST API authentication")
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
                    AppLog.e(AppLog.T.EDITOR, "Failed to encode Basic auth credentials", e)
                    null
                }
            } else {
                AppLog.w(AppLog.T.EDITOR, "Incomplete credentials for Basic authentication")
                null
            }
        }
    }

    private fun shouldUsePlugins(
        isFeatureEnabled: Boolean,
        isWPComSite: Boolean,
        isJetpackConnected: Boolean,
        applicationPassword: String?
    ): Boolean {
        // Enable plugins for:
        // 1. WP.com Simple sites (when feature is enabled)
        // 2. Jetpack-connected sites with application passwords (when feature is enabled)
        return isFeatureEnabled &&
                (isWPComSite || (isJetpackConnected && !applicationPassword.isNullOrEmpty()))
    }
}
