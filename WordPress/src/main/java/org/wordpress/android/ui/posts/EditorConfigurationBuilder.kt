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
        return settings.run {
            // Extract key values
            val postId = getSetting<Int>("postId").let { if (it == 0) -1 else it }
            val siteURL = getSetting<String>("siteURL") ?: ""
            val siteApiRoot = getSetting<String>("siteApiRoot") ?: ""
            val siteApiNamespace = getStringArray("siteApiNamespace")
            val firstNamespace = siteApiNamespace.firstOrNull() ?: ""
            val namespaceExcludedPaths = getStringArray("namespaceExcludedPaths")
            val cookies = getSetting<Map<String, String>>("cookies") ?: emptyMap()

            // Construct editor assets endpoint
            val editorAssetsEndpoint = if (firstNamespace.isNotEmpty() && siteApiRoot.isNotEmpty()) {
                "${siteApiRoot}wpcom/v2/${firstNamespace}editor-assets"
            } else {
                null
            }

            // Build EditorConfiguration
            EditorConfiguration.builder().apply {
                // Post data
                setTitle(getSetting<String>("postTitle") ?: "")
                setContent(getSetting<String>("postContent") ?: "")
                setPostId(postId)
                setPostType(getSetting<String>("postType"))

                // Site configuration
                setSiteURL(siteURL)
                setSiteApiRoot(siteApiRoot)
                setSiteApiNamespace(siteApiNamespace)
                setNamespaceExcludedPaths(namespaceExcludedPaths)

                // Authentication
                setAuthHeader(getSetting<String>("authHeader") ?: "")

                // Features
                setThemeStyles(getSettingOrDefault("themeStyles", false))
                setPlugins(getSettingOrDefault("plugins", false))
                setHideTitle(false)

                // Localization
                setLocale(getSetting<String>("locale") ?: "en")

                // Assets and caching
                setEnableAssetCaching(true)

                // Set cached asset hosts
                val siteHost = UrlUtils.getHost(siteURL)
                if (!siteHost.isNullOrEmpty()) {
                    setCachedAssetHosts(setOf("s0.wp.com", siteHost))
                } else {
                    setCachedAssetHosts(setOf("s0.wp.com"))
                }

                // Set editor assets endpoint if available
                if (editorAssetsEndpoint != null) {
                    setEditorAssetsEndpoint(editorAssetsEndpoint)
                }

                // Cookies
                setCookies(cookies)

                // Editor settings (null for warmup scenarios)
                setEditorSettings(editorSettings)
            }.build()
        }
    }

    // Type-safe settings accessors - moved from GutenbergKitEditorFragment
    private inline fun <reified T> Map<String, Any?>.getSetting(key: String): T? = this[key] as? T

    private inline fun <reified T> Map<String, Any?>.getSettingOrDefault(key: String, default: T): T =
        getSetting(key) ?: default

    private fun Map<String, Any?>.getStringArray(key: String): Array<String> =
        getSetting<Array<String?>>(key)?.asSequence()?.filterNotNull()?.toList()?.toTypedArray() ?: emptyArray()
}
