package org.wordpress.android.ui.posts.editor.config

import android.content.res.Configuration
import androidx.lifecycle.LifecycleOwner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editpost.repository.EditPostRepository
import org.wordpress.android.ui.posts.editor.state.EditorStateManager
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PHASE 3: Editor Configuration Management
 * 
 * Manages editor configuration and setup:
 * - Editor initialization and setup
 * - Configuration change handling
 * - Editor preferences and settings
 * - Theme and appearance management
 * - Toolbar and UI configuration
 * 
 * Extracted from EditPostActivity to centralize configuration logic.
 */
@Singleton
class EditorConfigurationManager @Inject constructor() {

    // Editor configuration data
    data class EditorConfiguration(
        val editorType: EditorStateManager.EditorType,
        val isToolbarExpanded: Boolean = false,
        val isDarkMode: Boolean = false,
        val fontSize: FontSize = FontSize.MEDIUM,
        val isFullScreenMode: Boolean = false,
        val enabledFeatures: Set<EditorFeature> = emptySet()
    )

    enum class FontSize {
        SMALL, MEDIUM, LARGE, EXTRA_LARGE
    }

    enum class EditorFeature {
        AUTO_SAVE,
        SPELL_CHECK,
        RICH_TEXT_FORMATTING,
        MEDIA_INSERTION,
        LINK_INSERTION,
        TABLE_SUPPORT,
        CODE_HIGHLIGHTING
    }

    // Configuration
    private var isInitialized = false
    private var listener: ConfigurationListener? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var editPostRepository: EditPostRepository? = null
    private var siteModel: SiteModel? = null
    
    // Current configuration
    private var currentConfiguration: EditorConfiguration? = null
    private var isConfigurationChangePending = false
    
    /**
     * Interface for configuration events
     */
    interface ConfigurationListener {
        fun onConfigurationLoaded(configuration: EditorConfiguration)
        fun onEditorConfigurationChanged(newConfiguration: EditorConfiguration, oldConfiguration: EditorConfiguration?)
        fun onConfigurationChangeHandled()
        fun onThemeChanged(isDarkMode: Boolean)
        fun onToolbarStateChanged(isExpanded: Boolean)
        fun onFullScreenModeChanged(isFullScreen: Boolean)
        fun showConfigurationError(error: String)
    }

    /**
     * Initialize the configuration manager
     */
    fun initialize(
        listener: ConfigurationListener,
        lifecycleOwner: LifecycleOwner,
        editPostRepository: EditPostRepository,
        siteModel: SiteModel
    ) {
        this.listener = listener
        this.lifecycleOwner = lifecycleOwner
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel
        this.isInitialized = true
        
        // Load initial configuration
        loadConfiguration()
        
        AppLog.d(AppLog.T.POSTS, "EditorConfigurationManager initialized")
    }

    /**
     * Load editor configuration
     */
    fun loadConfiguration() {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "EditorConfigurationManager not initialized")
            return
        }

        try {
            val configuration = createDefaultConfiguration()
            currentConfiguration = configuration
            
            AppLog.d(AppLog.T.POSTS, "Configuration loaded: $configuration")
            listener?.onConfigurationLoaded(configuration)
            
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error loading configuration: ${e.message}")
            listener?.showConfigurationError("Failed to load editor configuration: ${e.message}")
        }
    }

    /**
     * Update editor configuration
     */
    fun updateConfiguration(newConfiguration: EditorConfiguration) {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "EditorConfigurationManager not initialized")
            return
        }

        val oldConfiguration = currentConfiguration
        currentConfiguration = newConfiguration
        
        AppLog.d(AppLog.T.POSTS, "Configuration updated: $newConfiguration")
        listener?.onEditorConfigurationChanged(newConfiguration, oldConfiguration)
        
        // Save configuration
        saveConfiguration(newConfiguration)
    }

    /**
     * Handle device configuration changes
     */
    fun handleConfigurationChange(newConfig: Configuration) {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "EditorConfigurationManager not initialized")
            return
        }

        isConfigurationChangePending = true
        
        AppLog.d(AppLog.T.POSTS, "Handling configuration change")
        
        // Check if theme changed
        val isDarkMode = isSystemInDarkMode(newConfig)
        currentConfiguration?.let { config ->
            if (config.isDarkMode != isDarkMode) {
                updateTheme(isDarkMode)
            }
        }
        
        // Notify that configuration change is handled
        isConfigurationChangePending = false
        listener?.onConfigurationChangeHandled()
    }

    /**
     * Update editor theme
     */
    fun updateTheme(isDarkMode: Boolean) {
        currentConfiguration?.let { config ->
            val newConfig = config.copy(isDarkMode = isDarkMode)
            updateConfiguration(newConfig)
            listener?.onThemeChanged(isDarkMode)
        }
    }

    /**
     * Toggle toolbar expansion
     */
    fun toggleToolbarExpansion() {
        currentConfiguration?.let { config ->
            val newExpanded = !config.isToolbarExpanded
            val newConfig = config.copy(isToolbarExpanded = newExpanded)
            updateConfiguration(newConfig)
            listener?.onToolbarStateChanged(newExpanded)
        }
    }

    /**
     * Set toolbar expansion state
     */
    fun setToolbarExpanded(isExpanded: Boolean) {
        currentConfiguration?.let { config ->
            if (config.isToolbarExpanded != isExpanded) {
                val newConfig = config.copy(isToolbarExpanded = isExpanded)
                updateConfiguration(newConfig)
                listener?.onToolbarStateChanged(isExpanded)
            }
        }
    }

    /**
     * Toggle full screen mode
     */
    fun toggleFullScreenMode() {
        currentConfiguration?.let { config ->
            val newFullScreen = !config.isFullScreenMode
            val newConfig = config.copy(isFullScreenMode = newFullScreen)
            updateConfiguration(newConfig)
            listener?.onFullScreenModeChanged(newFullScreen)
        }
    }

    /**
     * Update font size
     */
    fun updateFontSize(fontSize: FontSize) {
        currentConfiguration?.let { config ->
            val newConfig = config.copy(fontSize = fontSize)
            updateConfiguration(newConfig)
        }
    }

    /**
     * Enable or disable a feature
     */
    fun setFeatureEnabled(feature: EditorFeature, enabled: Boolean) {
        currentConfiguration?.let { config ->
            val newFeatures = if (enabled) {
                config.enabledFeatures + feature
            } else {
                config.enabledFeatures - feature
            }
            val newConfig = config.copy(enabledFeatures = newFeatures)
            updateConfiguration(newConfig)
        }
    }

    /**
     * Check if a feature is enabled
     */
    fun isFeatureEnabled(feature: EditorFeature): Boolean {
        return currentConfiguration?.enabledFeatures?.contains(feature) ?: false
    }

    /**
     * Get current configuration
     */
    fun getCurrentConfiguration(): EditorConfiguration? = currentConfiguration

    /**
     * Create default configuration
     */
    private fun createDefaultConfiguration(): EditorConfiguration {
        return EditorConfiguration(
            editorType = EditorStateManager.EditorType.AZTEC,
            isToolbarExpanded = false,
            isDarkMode = false,
            fontSize = FontSize.MEDIUM,
            isFullScreenMode = false,
            enabledFeatures = setOf(
                EditorFeature.AUTO_SAVE,
                EditorFeature.RICH_TEXT_FORMATTING,
                EditorFeature.MEDIA_INSERTION,
                EditorFeature.LINK_INSERTION
            )
        )
    }

    /**
     * Save configuration to preferences
     */
    private fun saveConfiguration(configuration: EditorConfiguration) {
        // In real implementation, this would save to SharedPreferences
        // or other persistent storage
        AppLog.d(AppLog.T.POSTS, "Configuration saved (placeholder)")
    }

    /**
     * Load configuration from preferences
     */
    private fun loadConfigurationFromStorage(): EditorConfiguration? {
        // In real implementation, this would load from SharedPreferences
        // or other persistent storage
        return null
    }

    /**
     * Check if system is in dark mode
     */
    private fun isSystemInDarkMode(config: Configuration): Boolean {
        return (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Get configuration for specific editor type
     */
    fun getConfigurationForEditor(editorType: EditorStateManager.EditorType): EditorConfiguration {
        val baseConfig = currentConfiguration ?: createDefaultConfiguration()
        
        return when (editorType) {
            EditorStateManager.EditorType.GUTENBERG -> {
                baseConfig.copy(
                    editorType = editorType,
                    enabledFeatures = baseConfig.enabledFeatures + setOf(
                        EditorFeature.TABLE_SUPPORT,
                        EditorFeature.CODE_HIGHLIGHTING
                    )
                )
            }
            EditorStateManager.EditorType.GUTENBERG_KIT -> {
                baseConfig.copy(
                    editorType = editorType,
                    enabledFeatures = baseConfig.enabledFeatures + setOf(
                        EditorFeature.TABLE_SUPPORT,
                        EditorFeature.CODE_HIGHLIGHTING,
                        EditorFeature.SPELL_CHECK
                    )
                )
            }
            EditorStateManager.EditorType.AZTEC -> {
                baseConfig.copy(
                    editorType = editorType,
                    enabledFeatures = baseConfig.enabledFeatures - setOf(
                        EditorFeature.TABLE_SUPPORT,
                        EditorFeature.CODE_HIGHLIGHTING
                    )
                )
            }
        }
    }

    /**
     * Reset configuration to defaults
     */
    fun resetToDefaults() {
        val defaultConfig = createDefaultConfiguration()
        updateConfiguration(defaultConfig)
        AppLog.d(AppLog.T.POSTS, "Configuration reset to defaults")
    }

    /**
     * Export configuration
     */
    fun exportConfiguration(): Map<String, Any> {
        val config = currentConfiguration ?: return emptyMap()
        
        return mapOf(
            "editorType" to config.editorType.name,
            "isToolbarExpanded" to config.isToolbarExpanded,
            "isDarkMode" to config.isDarkMode,
            "fontSize" to config.fontSize.name,
            "isFullScreenMode" to config.isFullScreenMode,
            "enabledFeatures" to config.enabledFeatures.map { it.name }
        )
    }

    /**
     * Import configuration
     */
    fun importConfiguration(configMap: Map<String, Any>) {
        try {
            val editorType = EditorStateManager.EditorType.valueOf(
                configMap["editorType"] as? String ?: EditorStateManager.EditorType.AZTEC.name
            )
            val isToolbarExpanded = configMap["isToolbarExpanded"] as? Boolean ?: false
            val isDarkMode = configMap["isDarkMode"] as? Boolean ?: false
            val fontSize = FontSize.valueOf(
                configMap["fontSize"] as? String ?: FontSize.MEDIUM.name
            )
            val isFullScreenMode = configMap["isFullScreenMode"] as? Boolean ?: false
            
            @Suppress("UNCHECKED_CAST")
            val featureNames = configMap["enabledFeatures"] as? List<String> ?: emptyList()
            val enabledFeatures = featureNames.mapNotNull { name ->
                try {
                    EditorFeature.valueOf(name)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
            
            val importedConfig = EditorConfiguration(
                editorType = editorType,
                isToolbarExpanded = isToolbarExpanded,
                isDarkMode = isDarkMode,
                fontSize = fontSize,
                isFullScreenMode = isFullScreenMode,
                enabledFeatures = enabledFeatures
            )
            
            updateConfiguration(importedConfig)
            AppLog.d(AppLog.T.POSTS, "Configuration imported successfully")
            
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error importing configuration: ${e.message}")
            listener?.showConfigurationError("Failed to import configuration: ${e.message}")
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        currentConfiguration = null
        isConfigurationChangePending = false
        
        listener = null
        lifecycleOwner = null
        editPostRepository = null
        siteModel = null
        isInitialized = false
        
        AppLog.d(AppLog.T.POSTS, "EditorConfigurationManager cleaned up")
    }
}
