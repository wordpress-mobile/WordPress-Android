package org.wordpress.android.ui.posts

import org.wordpress.android.util.UrlUtils
import org.wordpress.gutenberg.EditorConfiguration

/**
 * Utility object for building EditorConfiguration from settings maps.
 * Eliminates duplication between GutenbergKitEditorFragment and GutenbergKitWarmupHelper.
 */
object EditorConfigurationBuilder {
    /**
     * Builds an EditorConfiguration from the provided settings map.
     *
     * @param settings The settings map containing all configuration values
     * @param editorSettings Optional editor settings string (null for warmup scenarios)
     * @return Configured EditorConfiguration instance
     */
    fun build(
        settings: Map<String, Any?>,
        editorSettings: String? = null
    ): EditorConfiguration {
        return EditorConfiguration.Builder().apply {
            val postId = settings.getSetting<Int>("postId")?.let { if (it == 0) -1 else it }
            val siteURL = settings.getSetting<String>("siteURL") ?: ""
            val siteApiNamespace = settings.getStringArray("siteApiNamespace")

            // Post settings
            setTitle(settings.getSetting<String>("postTitle") ?: "")
            setContent(settings.getSetting<String>("postContent") ?: "")
            setPostId(postId)
            setPostType(settings.getSetting<String>("postType"))

            // Site settings
            setSiteURL(siteURL)
            setSiteApiRoot(settings.getSetting<String>("siteApiRoot") ?: "")
            setSiteApiNamespace(siteApiNamespace)
            setNamespaceExcludedPaths(settings.getStringArray("namespaceExcludedPaths"))
            setAuthHeader(settings.getSetting<String>("authHeader") ?: "")

            // Features
            setThemeStyles(settings.getSettingOrDefault("themeStyles", false))
            setPlugins(settings.getSettingOrDefault("plugins", false))
            setLocale(settings.getSetting<String>("locale") ?: "en")

            // Editor asset caching configuration
            configureEditorAssetCaching(settings, siteURL, siteApiNamespace)

            // Cookies
            setCookies(settings.getSetting<Map<String, String>>("cookies") ?: emptyMap())

            // Network logging for debugging
            setEnableNetworkLogging(settings.getSettingOrDefault("enableNetworkLogging", false))

            // Editor settings (null for warmup scenarios)
            setEditorSettings(editorSettings)
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
        val siteApiRoot = settings.getSetting<String>("siteApiRoot") ?: ""
        if (firstNamespace.isNotEmpty() && siteApiRoot.isNotEmpty()) {
            setEditorAssetsEndpoint("${siteApiRoot}wpcom/v2/${firstNamespace}editor-assets")
        }
    }

    // Type-safe settings accessors - moved from GutenbergKitEditorFragment
    private inline fun <reified T> Map<String, Any?>.getSetting(key: String): T? = this[key] as? T

    private inline fun <reified T> Map<String, Any?>.getSettingOrDefault(key: String, default: T): T =
        getSetting(key) ?: default

    private fun Map<String, Any?>.getStringArray(key: String): Array<String> =
        getSetting<Array<String?>>(key)?.asSequence()?.filterNotNull()?.toList()?.toTypedArray() ?: emptyArray()
}
