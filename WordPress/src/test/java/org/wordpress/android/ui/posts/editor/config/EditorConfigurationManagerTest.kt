package org.wordpress.android.ui.posts.editor.config

import android.content.res.Configuration
import androidx.lifecycle.LifecycleOwner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.state.EditorStateManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for EditorConfigurationManager.
 * Tests the configuration management logic extracted from EditPostActivity.
 */
@RunWith(MockitoJUnitRunner::class)
class EditorConfigurationManagerTest {

    @Mock private lateinit var listener: EditorConfigurationManager.ConfigurationListener
    @Mock private lateinit var lifecycleOwner: LifecycleOwner
    @Mock private lateinit var editPostRepository: EditPostRepository
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var configuration: Configuration

    private lateinit var configurationManager: EditorConfigurationManager

    @Before
    fun setUp() {
        configurationManager = EditorConfigurationManager()
    }

    @Test
    fun `initialize should load default configuration`() {
        // Act
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Assert
        verify(listener).onConfigurationLoaded(any())
        assertNotNull(configurationManager.getCurrentConfiguration())
    }

    @Test
    fun `loadConfiguration should create default configuration`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act
        configurationManager.loadConfiguration()

        // Assert
        val config = configurationManager.getCurrentConfiguration()
        assertNotNull(config)
        assertEquals(EditorStateManager.EditorType.AZTEC, config.editorType)
        assertFalse(config.isToolbarExpanded)
        assertFalse(config.isDarkMode)
        assertEquals(EditorConfigurationManager.FontSize.MEDIUM, config.fontSize)
        assertFalse(config.isFullScreenMode)
        assertTrue(config.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.AUTO_SAVE))
        assertTrue(config.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.RICH_TEXT_FORMATTING))
    }

    @Test
    fun `updateConfiguration should notify listener and save configuration`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        val oldConfig = configurationManager.getCurrentConfiguration()
        val newConfig = oldConfig!!.copy(isToolbarExpanded = true)

        // Act
        configurationManager.updateConfiguration(newConfig)

        // Assert
        verify(listener).onConfigurationChanged(newConfig, oldConfig)
        assertEquals(newConfig, configurationManager.getCurrentConfiguration())
        assertTrue(configurationManager.getCurrentConfiguration()!!.isToolbarExpanded)
    }

    @Test
    fun `updateTheme should update dark mode and notify listener`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        clearInvocations(listener)

        // Act
        configurationManager.updateTheme(true)

        // Assert
        verify(listener).onThemeChanged(true)
        verify(listener).onConfigurationChanged(any(), any())
        assertTrue(configurationManager.getCurrentConfiguration()!!.isDarkMode)
    }

    @Test
    fun `toggleToolbarExpansion should toggle state and notify listener`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        clearInvocations(listener)
        assertFalse(configurationManager.getCurrentConfiguration()!!.isToolbarExpanded)

        // Act
        configurationManager.toggleToolbarExpansion()

        // Assert
        verify(listener).onToolbarStateChanged(true)
        assertTrue(configurationManager.getCurrentConfiguration()!!.isToolbarExpanded)

        // Act again
        configurationManager.toggleToolbarExpansion()

        // Assert
        verify(listener).onToolbarStateChanged(false)
        assertFalse(configurationManager.getCurrentConfiguration()!!.isToolbarExpanded)
    }

    @Test
    fun `setToolbarExpanded should set state and notify listener only when changed`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        clearInvocations(listener)

        // Act - set to true
        configurationManager.setToolbarExpanded(true)

        // Assert
        verify(listener).onToolbarStateChanged(true)
        assertTrue(configurationManager.getCurrentConfiguration()!!.isToolbarExpanded)

        // Act - set to true again (no change)
        clearInvocations(listener)
        configurationManager.setToolbarExpanded(true)

        // Assert - should not notify
        verify(listener, never()).onToolbarStateChanged(any())
    }

    @Test
    fun `toggleFullScreenMode should toggle state and notify listener`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        clearInvocations(listener)
        assertFalse(configurationManager.getCurrentConfiguration()!!.isFullScreenMode)

        // Act
        configurationManager.toggleFullScreenMode()

        // Assert
        verify(listener).onFullScreenModeChanged(true)
        assertTrue(configurationManager.getCurrentConfiguration()!!.isFullScreenMode)
    }

    @Test
    fun `updateFontSize should update font size`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        assertEquals(EditorConfigurationManager.FontSize.MEDIUM, configurationManager.getCurrentConfiguration()!!.fontSize)

        // Act
        configurationManager.updateFontSize(EditorConfigurationManager.FontSize.LARGE)

        // Assert
        assertEquals(EditorConfigurationManager.FontSize.LARGE, configurationManager.getCurrentConfiguration()!!.fontSize)
    }

    @Test
    fun `setFeatureEnabled should enable and disable features`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        assertFalse(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.SPELL_CHECK))

        // Act - enable feature
        configurationManager.setFeatureEnabled(EditorConfigurationManager.EditorFeature.SPELL_CHECK, true)

        // Assert
        assertTrue(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.SPELL_CHECK))

        // Act - disable feature
        configurationManager.setFeatureEnabled(EditorConfigurationManager.EditorFeature.SPELL_CHECK, false)

        // Assert
        assertFalse(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.SPELL_CHECK))
    }

    @Test
    fun `isFeatureEnabled should return correct state for default features`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Assert default enabled features
        assertTrue(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.AUTO_SAVE))
        assertTrue(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.RICH_TEXT_FORMATTING))
        assertTrue(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.MEDIA_INSERTION))
        assertTrue(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.LINK_INSERTION))

        // Assert default disabled features
        assertFalse(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.SPELL_CHECK))
        assertFalse(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.TABLE_SUPPORT))
        assertFalse(configurationManager.isFeatureEnabled(EditorConfigurationManager.EditorFeature.CODE_HIGHLIGHTING))
    }

    @Test
    fun `getConfigurationForEditor should return editor-specific configuration`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act & Assert - Gutenberg
        val gutenbergConfig = configurationManager.getConfigurationForEditor(EditorStateManager.EditorType.GUTENBERG)
        assertEquals(EditorStateManager.EditorType.GUTENBERG, gutenbergConfig.editorType)
        assertTrue(gutenbergConfig.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.TABLE_SUPPORT))
        assertTrue(gutenbergConfig.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.CODE_HIGHLIGHTING))

        // Act & Assert - Gutenberg Kit
        val gutenbergKitConfig = configurationManager.getConfigurationForEditor(EditorStateManager.EditorType.GUTENBERG_KIT)
        assertEquals(EditorStateManager.EditorType.GUTENBERG_KIT, gutenbergKitConfig.editorType)
        assertTrue(gutenbergKitConfig.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.TABLE_SUPPORT))
        assertTrue(gutenbergKitConfig.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.CODE_HIGHLIGHTING))
        assertTrue(gutenbergKitConfig.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.SPELL_CHECK))

        // Act & Assert - Aztec
        val aztecConfig = configurationManager.getConfigurationForEditor(EditorStateManager.EditorType.AZTEC)
        assertEquals(EditorStateManager.EditorType.AZTEC, aztecConfig.editorType)
        assertFalse(aztecConfig.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.TABLE_SUPPORT))
        assertFalse(aztecConfig.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.CODE_HIGHLIGHTING))
    }

    @Test
    fun `handleConfigurationChange should handle dark mode changes`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        clearInvocations(listener)
        
        // Mock dark mode configuration
        whenever(configuration.uiMode).thenReturn(Configuration.UI_MODE_NIGHT_YES)

        // Act
        configurationManager.handleConfigurationChange(configuration)

        // Assert
        verify(listener).onThemeChanged(true)
        verify(listener).onConfigurationChangeHandled()
        assertTrue(configurationManager.getCurrentConfiguration()!!.isDarkMode)
    }

    @Test
    fun `handleConfigurationChange should handle light mode changes`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        configurationManager.updateTheme(true) // Start with dark mode
        clearInvocations(listener)
        
        // Mock light mode configuration
        whenever(configuration.uiMode).thenReturn(Configuration.UI_MODE_NIGHT_NO)

        // Act
        configurationManager.handleConfigurationChange(configuration)

        // Assert
        verify(listener).onThemeChanged(false)
        verify(listener).onConfigurationChangeHandled()
        assertFalse(configurationManager.getCurrentConfiguration()!!.isDarkMode)
    }

    @Test
    fun `resetToDefaults should restore default configuration`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        
        // Modify configuration
        configurationManager.setToolbarExpanded(true)
        configurationManager.updateTheme(true)
        configurationManager.updateFontSize(EditorConfigurationManager.FontSize.LARGE)
        assertTrue(configurationManager.getCurrentConfiguration()!!.isToolbarExpanded)
        assertTrue(configurationManager.getCurrentConfiguration()!!.isDarkMode)
        assertEquals(EditorConfigurationManager.FontSize.LARGE, configurationManager.getCurrentConfiguration()!!.fontSize)
        
        clearInvocations(listener)

        // Act
        configurationManager.resetToDefaults()

        // Assert
        verify(listener).onConfigurationChanged(any(), any())
        val config = configurationManager.getCurrentConfiguration()!!
        assertFalse(config.isToolbarExpanded)
        assertFalse(config.isDarkMode)
        assertEquals(EditorConfigurationManager.FontSize.MEDIUM, config.fontSize)
    }

    @Test
    fun `exportConfiguration should return configuration map`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        configurationManager.setToolbarExpanded(true)
        configurationManager.updateTheme(true)

        // Act
        val exported = configurationManager.exportConfiguration()

        // Assert
        assertEquals("AZTEC", exported["editorType"])
        assertEquals(true, exported["isToolbarExpanded"])
        assertEquals(true, exported["isDarkMode"])
        assertEquals("MEDIUM", exported["fontSize"])
        assertEquals(false, exported["isFullScreenMode"])
        assertTrue(exported["enabledFeatures"] is List<*>)
    }

    @Test
    fun `importConfiguration should restore configuration from map`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        clearInvocations(listener)
        
        val configMap = mapOf(
            "editorType" to "GUTENBERG",
            "isToolbarExpanded" to true,
            "isDarkMode" to true,
            "fontSize" to "LARGE",
            "isFullScreenMode" to true,
            "enabledFeatures" to listOf("AUTO_SAVE", "SPELL_CHECK")
        )

        // Act
        configurationManager.importConfiguration(configMap)

        // Assert
        verify(listener).onConfigurationChanged(any(), any())
        val config = configurationManager.getCurrentConfiguration()!!
        assertEquals(EditorStateManager.EditorType.GUTENBERG, config.editorType)
        assertTrue(config.isToolbarExpanded)
        assertTrue(config.isDarkMode)
        assertEquals(EditorConfigurationManager.FontSize.LARGE, config.fontSize)
        assertTrue(config.isFullScreenMode)
        assertTrue(config.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.AUTO_SAVE))
        assertTrue(config.enabledFeatures.contains(EditorConfigurationManager.EditorFeature.SPELL_CHECK))
    }

    @Test
    fun `importConfiguration should handle invalid configuration gracefully`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        
        val invalidConfigMap = mapOf(
            "editorType" to "INVALID_TYPE",
            "fontSize" to "INVALID_SIZE",
            "enabledFeatures" to listOf("INVALID_FEATURE")
        )

        // Act
        configurationManager.importConfiguration(invalidConfigMap)

        // Assert
        verify(listener).showConfigurationError(contains("Failed to import configuration"))
    }

    @Test
    fun `cleanup should reset all state`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        configurationManager.setToolbarExpanded(true)

        // Act
        configurationManager.cleanup()

        // Assert
        assertEquals(null, configurationManager.getCurrentConfiguration())
    }

    @Test
    fun `operations after cleanup should handle gracefully`() {
        // Arrange
        configurationManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        configurationManager.cleanup()

        // Act & Assert - should not crash
        configurationManager.loadConfiguration()
        configurationManager.updateTheme(true)
        configurationManager.toggleToolbarExpansion()
        configurationManager.handleConfigurationChange(configuration)
    }
}
