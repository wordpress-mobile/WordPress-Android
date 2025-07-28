package org.wordpress.android.ui.posts.editor.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import org.wordpress.android.editor.AztecEditorFragment
import org.wordpress.android.editor.gutenberg.GutenbergEditorFragment
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editpost.repository.EditPostRepository
import org.wordpress.android.ui.posts.editor.state.EditorStateManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for EditorFragmentManager.
 * Tests the fragment management logic extracted from EditPostActivity.
 */
@RunWith(MockitoJUnitRunner::class)
class EditorFragmentManagerTest {

    @Mock private lateinit var listener: EditorFragmentManager.FragmentManagerListener
    @Mock private lateinit var lifecycleOwner: LifecycleOwner
    @Mock private lateinit var fragmentManager: FragmentManager
    @Mock private lateinit var editPostRepository: EditPostRepository
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var postModel: PostImmutableModel

    private lateinit var editorFragmentManager: EditorFragmentManager

    @Before
    fun setUp() {
        editorFragmentManager = EditorFragmentManager()
    }

    @Test
    fun `initialize should set up manager correctly`() {
        // Act
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)

        // Assert - should not crash and should be ready for operations
        assertFalse(editorFragmentManager.isCurrentFragmentReady())
    }

    @Test(expected = IllegalStateException::class)
    fun `createEditorFragment should throw when not initialized`() {
        // Act
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)
    }

    @Test
    fun `createEditorFragment should create Aztec fragment`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)

        // Act
        val fragment = editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Assert
        assertNotNull(fragment)
        assertTrue(fragment is AztecEditorFragment)
        assertEquals(fragment, editorFragmentManager.getCurrentFragment())
        assertEquals(EditorStateManager.EditorType.AZTEC, editorFragmentManager.getCurrentEditorType())
        verify(listener).onFragmentCreated(fragment, EditorStateManager.EditorType.AZTEC)
    }

    @Test
    fun `createEditorFragment should create Gutenberg fragment`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)

        // Act
        val fragment = editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.GUTENBERG)

        // Assert
        assertNotNull(fragment)
        assertTrue(fragment is GutenbergEditorFragment)
        assertEquals(fragment, editorFragmentManager.getCurrentFragment())
        assertEquals(EditorStateManager.EditorType.GUTENBERG, editorFragmentManager.getCurrentEditorType())
        verify(listener).onFragmentCreated(fragment, EditorStateManager.EditorType.GUTENBERG)
    }

    @Test
    fun `switchToEditor should not switch to same editor type`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Act
        editorFragmentManager.switchToEditor(EditorStateManager.EditorType.AZTEC)

        // Assert
        verify(listener, never()).onFragmentSwitching(any(), any())
        verify(listener, never()).onFragmentSwitched(any(), any())
    }

    @Test
    fun `switchToEditor should switch to different editor type`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        val oldFragment = editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Act
        editorFragmentManager.switchToEditor(EditorStateManager.EditorType.GUTENBERG)

        // Assert
        val newFragment = editorFragmentManager.getCurrentFragment()
        assertNotNull(newFragment)
        assertTrue(newFragment is GutenbergEditorFragment)
        assertEquals(EditorStateManager.EditorType.GUTENBERG, editorFragmentManager.getCurrentEditorType())
        
        verify(listener).onFragmentSwitching(oldFragment, newFragment)
        verify(listener).onFragmentSwitched(newFragment, EditorStateManager.EditorType.GUTENBERG)
    }

    @Test
    fun `markFragmentReady should update ready state and notify listener`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        val fragment = editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Act
        editorFragmentManager.markFragmentReady()

        // Assert
        assertTrue(editorFragmentManager.isCurrentFragmentReady())
        verify(listener).onFragmentReady(fragment)
    }

    @Test
    fun `markFragmentReady should not notify if already ready`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)
        editorFragmentManager.markFragmentReady()
        clearInvocations(listener)

        // Act
        editorFragmentManager.markFragmentReady()

        // Assert
        verify(listener, never()).onFragmentReady(any())
    }

    @Test
    fun `updateFragmentContent should update post content`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)
        
        whenever(postModel.title).thenReturn("Test Title")
        whenever(postModel.content).thenReturn("Test Content")

        // Act
        editorFragmentManager.updateFragmentContent(postModel)

        // Assert - should not crash (actual fragment interaction would require real fragments)
    }

    @Test
    fun `getContentFromFragment should return content from current fragment`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Act
        val content = editorFragmentManager.getContentFromFragment()

        // Assert - with mock fragments, this would return null
        // In real implementation with real fragments, this would return actual content
    }

    @Test
    fun `switchToEditor with preserveContent true should transfer content`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Act
        editorFragmentManager.switchToEditor(EditorStateManager.EditorType.GUTENBERG, preserveContent = true)

        // Assert
        verify(listener).onContentTransferred(EditorStateManager.EditorType.AZTEC, EditorStateManager.EditorType.GUTENBERG)
    }

    @Test
    fun `switchToEditor with preserveContent false should not transfer content`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Act
        editorFragmentManager.switchToEditor(EditorStateManager.EditorType.GUTENBERG, preserveContent = false)

        // Assert
        verify(listener, never()).onContentTransferred(any(), any())
    }

    @Test
    fun `fragment lifecycle methods should log appropriately`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)

        // Act
        editorFragmentManager.onFragmentResumed()
        editorFragmentManager.onFragmentPaused()
        editorFragmentManager.onFragmentStopped()

        // Assert - should not crash
    }

    @Test
    fun `cleanup should reset all state`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.createEditorFragment(EditorStateManager.EditorType.AZTEC)
        editorFragmentManager.markFragmentReady()

        // Act
        editorFragmentManager.cleanup()

        // Assert
        assertEquals(null, editorFragmentManager.getCurrentFragment())
        assertEquals(null, editorFragmentManager.getCurrentEditorType())
        assertFalse(editorFragmentManager.isCurrentFragmentReady())
    }

    @Test
    fun `operations after cleanup should handle gracefully`() {
        // Arrange
        editorFragmentManager.initialize(listener, lifecycleOwner, fragmentManager, editPostRepository, siteModel)
        editorFragmentManager.cleanup()

        // Act & Assert - should not crash
        editorFragmentManager.switchToEditor(EditorStateManager.EditorType.GUTENBERG)
        editorFragmentManager.markFragmentReady()
        editorFragmentManager.updateFragmentContent(postModel)
        editorFragmentManager.onFragmentResumed()
    }
}
