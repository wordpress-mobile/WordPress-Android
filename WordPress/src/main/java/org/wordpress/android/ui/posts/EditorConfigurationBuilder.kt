package org.wordpress.android.ui.posts

import org.wordpress.android.util.UrlUtils
import org.wordpress.gutenberg.model.EditorConfiguration
import org.wordpress.gutenberg.model.PostTypeDetails

/**
 * Utility object for building EditorConfiguration from settings maps.
 * Eliminates duplication between GutenbergKitEditorFragment and GutenbergKitWarmupHelper.
 */
object EditorConfigurationBuilder {
    /**
     * Builds an EditorConfiguration from the provided settings map.
     *
     * @param settings The settings map containing all configuration values
     * @return Configured EditorConfiguration instance
     */
    fun build(
        settings: Map<String, Any?>,
    ): EditorConfiguration {
        val siteURL = settings.getSetting<String>("siteURL") ?: ""
        val siteApiRoot = settings.getSetting<String>("siteApiRoot") ?: ""
        val postType = settings.getSetting<PostTypeDetails>("postType")
            ?: PostTypeDetails.post
        val siteApiNamespace = settings.getStringArray("siteApiNamespace")

        return EditorConfiguration.builder(
            siteURL = siteURL,
            siteApiRoot = siteApiRoot,
            postType = postType
        ).apply {
            val postId = settings.getSetting<Int>("postId")
                ?.let { if (it == 0) null else it.toUInt() }

            // Post settings
            setTitle(settings.getSetting<String>("postTitle") ?: "")
            setContent(settings.getSetting<String>("postContent") ?: "")
            setPostId(postId)
            setPostStatus(settings.getSetting<String>("status") ?: "draft")

            // Site settings
            setSiteApiNamespace(siteApiNamespace)
            setNamespaceExcludedPaths(
                settings.getStringArray("namespaceExcludedPaths")
            )
            setAuthHeader(
                settings.getSetting<String>("authHeader") ?: ""
            )

            // Features
            setThemeStyles(
                settings.getSettingOrDefault("themeStyles", false)
            )
            setPlugins(
                settings.getSettingOrDefault("plugins", false)
            )
            setLocale(settings.getSetting<String>("locale") ?: "en")

            // Editor asset caching configuration
            configureEditorAssetCaching(
                settings, siteURL, siteApiNamespace
            )

            // Cookies
            setCookies(
                settings.getSetting<Map<String, String>>("cookies")
                    ?: emptyMap()
            )

            // Network logging for debugging
            setEnableNetworkLogging(
                settings.getSettingOrDefault("enableNetworkLogging", false)
            )
        }.build()
    }

    private fun EditorConfiguration.Builder.configureEditorAssetCaching(
        settings: Map<String, Any?>,
        siteURL: String,
        siteApiNamespace: Array<String>
    ) {
        setEnableAssetCaching(true)

        val siteHost = UrlUtils.getHost(siteURL)
        val cachedHosts = if (!siteHost.isNullOrEmpty()) {
            setOf("s0.wp.com", siteHost)
        } else {
            setOf("s0.wp.com")
        }
        setCachedAssetHosts(cachedHosts)

        val firstNamespace = siteApiNamespace.firstOrNull() ?: ""
        val siteApiRoot =
            settings.getSetting<String>("siteApiRoot") ?: ""
        if (firstNamespace.isNotEmpty() && siteApiRoot.isNotEmpty()) {
            setEditorAssetsEndpoint(
                "${siteApiRoot}wpcom/v2/${firstNamespace}editor-assets"
            )
        }
    }

    // Type-safe settings accessors
    private inline fun <reified T> Map<String, Any?>.getSetting(
        key: String
    ): T? = this[key] as? T

    private inline fun <reified T> Map<String, Any?>.getSettingOrDefault(
        key: String, default: T
    ): T = getSetting(key) ?: default

    private fun Map<String, Any?>.getStringArray(
        key: String
    ): Array<String> =
        getSetting<Array<String?>>(key)
            ?.asSequence()?.filterNotNull()?.toList()?.toTypedArray()
            ?: emptyArray()
}
