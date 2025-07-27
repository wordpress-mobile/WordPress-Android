package org.wordpress.android.ui.posts.editor.state

import androidx.lifecycle.LifecycleOwner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for EditorStateManager.
 * Tests the state management logic extracted from EditPostActivity.
 */
@RunWith(MockitoJUnitRunner::class)
class EditorStateManagerTest {

    @Mock private lateinit var listener: EditorStateManager.EditorStateListener
    @Mock private lateinit var lifecycleOwner: LifecycleOwner
    @Mock private lateinit var editPostRepository: EditPostRepository
    @Mock private lateinit var siteModel: SiteModel

    private lateinit var editorStateManager: EditorStateManager

    @Before
    fun setUp() {
        editorStateManager = EditorStateManager()
    }

    @Test
    fun `initialize should set up manager correctly`() {
        // Act
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Assert
        assertEquals(EditorStateManager.EditorType.AZTEC, editorStateManager.getCurrentEditorType())
        assertEquals(EditorStateManager.EditorMode.VISUAL, editorStateManager.getCurrentEditorMode())
        assertFalse(editorStateManager.isEditorLoading())
        assertFalse(editorStateManager.hasPendingSwitch())
    }

    @Test
    fun `determineEditorType should return GUTENBERG for WPCom site`() {
        // Arrange
        whenever(siteModel.isWPCom).thenReturn(true)

        // Act
        val result = editorStateManager.determineEditorType(siteModel)

        // Assert
        assertEquals(EditorStateManager.EditorType.GUTENBERG, result)
    }

    @Test
    fun `determineEditorType should return GUTENBERG for WPComAtomic site`() {
        // Arrange
        whenever(siteModel.isWPCom).thenReturn(false)
        whenever(siteModel.isWPComAtomic).thenReturn(true)

        // Act
        val result = editorStateManager.determineEditorType(siteModel)

        // Assert
        assertEquals(EditorStateManager.EditorType.GUTENBERG, result)
    }

    @Test
    fun `determineEditorType should return AZTEC for self-hosted site`() {
        // Arrange
        whenever(siteModel.isWPCom).thenReturn(false)
        whenever(siteModel.isWPComAtomic).thenReturn(false)

        // Act
        val result = editorStateManager.determineEditorType(siteModel)

        // Assert
        assertEquals(EditorStateManager.EditorType.AZTEC, result)
    }

    @Test
    fun `requestEditorSwitch should not switch to same editor type`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.AZTEC)

        // Assert
        verify(listener, never()).onEditorSwitchRequested(any(), any())
        assertFalse(editorStateManager.hasPendingSwitch())
    }

    @Test
    fun `requestEditorSwitch should request switch for different editor type when no content`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        whenever(editPostRepository.hasPost()).thenReturn(false)

        // Act
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.GUTENBERG)

        // Assert
        verify(listener).onEditorSwitchRequested(EditorStateManager.EditorType.AZTEC, EditorStateManager.EditorType.GUTENBERG)
        assertTrue(editorStateManager.hasPendingSwitch())
        assertEquals(EditorStateManager.EditorType.GUTENBERG, editorStateManager.getPendingSwitch())
    }

    @Test
    fun `requestEditorSwitch should show dialog when content exists`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        whenever(editPostRepository.hasPost()).thenReturn(true)
        whenever(editPostRepository.content).thenReturn("Some content")

        // Act
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.GUTENBERG)

        // Assert
        verify(listener).showEditorSwitchDialog(EditorStateManager.EditorType.AZTEC, EditorStateManager.EditorType.GUTENBERG)
        assertFalse(editorStateManager.hasPendingSwitch())
    }

    @Test
    fun `confirmEditorSwitch should execute pending switch`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        whenever(editPostRepository.hasPost()).thenReturn(false)
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.GUTENBERG)

        // Act
        editorStateManager.confirmEditorSwitch()

        // Assert
        verify(listener).onEditorTypeChanged(EditorStateManager.EditorType.GUTENBERG, EditorStateManager.EditorType.AZTEC)
        assertEquals(EditorStateManager.EditorType.GUTENBERG, editorStateManager.getCurrentEditorType())
        assertFalse(editorStateManager.hasPendingSwitch())
    }

    @Test
    fun `cancelEditorSwitch should cancel pending switch`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        whenever(editPostRepository.hasPost()).thenReturn(false)
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.GUTENBERG)
        assertTrue(editorStateManager.hasPendingSwitch())

        // Act
        editorStateManager.cancelEditorSwitch()

        // Assert
        assertFalse(editorStateManager.hasPendingSwitch())
        assertEquals(EditorStateManager.EditorType.AZTEC, editorStateManager.getCurrentEditorType())
    }

    @Test
    fun `switchEditorMode should change mode and notify listener`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act
        editorStateManager.switchEditorMode(EditorStateManager.EditorMode.HTML)

        // Assert
        verify(listener).onEditorModeChanged(EditorStateManager.EditorMode.HTML, EditorStateManager.EditorMode.VISUAL)
        assertEquals(EditorStateManager.EditorMode.HTML, editorStateManager.getCurrentEditorMode())
    }

    @Test
    fun `switchEditorMode should not change to same mode`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act
        editorStateManager.switchEditorMode(EditorStateManager.EditorMode.VISUAL)

        // Assert
        verify(listener, never()).onEditorModeChanged(any(), any())
    }

    @Test
    fun `setEditorLoading should change loading state and notify listener`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act
        editorStateManager.setEditorLoading(true)

        // Assert
        verify(listener).onEditorLoadingStateChanged(true)
        assertTrue(editorStateManager.isEditorLoading())
    }

    @Test
    fun `setEditorLoading should not notify if state unchanged`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act
        editorStateManager.setEditorLoading(false)

        // Assert
        verify(listener, never()).onEditorLoadingStateChanged(any())
    }

    @Test
    fun `handleConfigurationChange should notify listener`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Act
        editorStateManager.handleConfigurationChange()

        // Assert
        verify(listener).onConfigurationChangeHandled()
    }

    @Test
    fun `getEditorDisplayName should return correct names`() {
        // Assert
        assertEquals("Gutenberg", editorStateManager.getEditorDisplayName(EditorStateManager.EditorType.GUTENBERG))
        assertEquals("Gutenberg Kit", editorStateManager.getEditorDisplayName(EditorStateManager.EditorType.GUTENBERG_KIT))
        assertEquals("Aztec", editorStateManager.getEditorDisplayName(EditorStateManager.EditorType.AZTEC))
    }

    @Test
    fun `supportsFeature should return correct support for different editors`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)

        // Test Aztec
        assertFalse(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.BLOCKS))
        assertTrue(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.HTML_MODE))
        assertTrue(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.MEDIA_UPLOAD))
        assertFalse(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.REAL_TIME_COLLABORATION))

        // Switch to Gutenberg
        whenever(editPostRepository.hasPost()).thenReturn(false)
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.GUTENBERG)
        editorStateManager.confirmEditorSwitch()

        // Test Gutenberg
        assertTrue(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.BLOCKS))
        assertTrue(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.HTML_MODE))
        assertTrue(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.MEDIA_UPLOAD))
        assertFalse(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.REAL_TIME_COLLABORATION))
    }

    @Test
    fun `supportsFeature should return true for collaboration in Gutenberg Kit`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        whenever(editPostRepository.hasPost()).thenReturn(false)
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.GUTENBERG_KIT)
        editorStateManager.confirmEditorSwitch()

        // Assert
        assertTrue(editorStateManager.supportsFeature(EditorStateManager.EditorFeature.REAL_TIME_COLLABORATION))
    }

    @Test
    fun `cleanup should reset state`() {
        // Arrange
        editorStateManager.initialize(listener, lifecycleOwner, editPostRepository, siteModel)
        whenever(editPostRepository.hasPost()).thenReturn(false)
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.GUTENBERG)

        // Act
        editorStateManager.cleanup()

        // Assert - operations should handle uninitialized state gracefully
        editorStateManager.setEditorLoading(true)
        editorStateManager.switchEditorMode(EditorStateManager.EditorMode.HTML)
        editorStateManager.requestEditorSwitch(EditorStateManager.EditorType.AZTEC)
        editorStateManager.handleConfigurationChange()

        // Should not crash or call listener after cleanup
        verifyNoMoreInteractions(listener)
    }
}
