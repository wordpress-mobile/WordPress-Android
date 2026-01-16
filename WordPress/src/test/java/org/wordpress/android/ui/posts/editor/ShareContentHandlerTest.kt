@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.editor

import android.content.Intent
import android.net.Uri
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.media.EditorMedia

@RunWith(MockitoJUnitRunner.Silent::class)
class ShareContentHandlerTest {
    @Mock
    private lateinit var editorMedia: EditorMedia

    @Mock
    private lateinit var listener: ShareContentHandler.ShareContentListener

    @Mock
    private lateinit var intent: Intent

    @Mock
    private lateinit var editPostRepository: EditPostRepository

    @Mock
    private lateinit var editorFragment: EditorFragmentAbstract

    private lateinit var handler: ShareContentHandler

    @Before
    fun setUp() {
        handler = ShareContentHandler(editorMedia)
        whenever(listener.getIntent()).thenReturn(intent)
        whenever(listener.getEditPostRepository()).thenReturn(editPostRepository)
        whenever(listener.getEditorFragment()).thenReturn(editorFragment)
        whenever(listener.isShowGutenbergEditor()).thenReturn(false)
        handler.start(listener)
    }

    // region setPostContentFromShareAction tests
    @Test
    fun `setPostContentFromShareAction sets hasSetPostContent when text is shared`() {
        // Arrange
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("Shared text content")
        whenever(intent.getStringExtra(Intent.EXTRA_SUBJECT)).thenReturn(null)
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(false)

        // Act
        handler.setPostContentFromShareAction()

        // Assert
        verify(listener).setHasSetPostContent(true)
        verify(editPostRepository).updateAsync(any(), any())
    }

    @Test
    fun `setPostContentFromShareAction does not set content when text is null`() {
        // Arrange
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(null)
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(false)

        // Act
        handler.setPostContentFromShareAction()

        // Assert
        verify(listener, never()).setHasSetPostContent(any())
        verify(editPostRepository, never()).updateAsync(any(), any())
    }

    @Test
    fun `setPostContentFromShareAction calls setPostMediaFromShareAction`() {
        // Arrange
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(null)
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(false)

        // Act
        handler.setPostContentFromShareAction()

        // Assert
        // setPostMediaFromShareAction is called internally, verifies no media added when no EXTRA_STREAM
        verify(editorMedia, never()).addNewMediaItemsToEditorAsync(any(), any())
    }
    // endregion

    // region setPostMediaFromShareAction tests
    @Test
    fun `setPostMediaFromShareAction does nothing when EXTRA_STREAM is missing`() {
        // Arrange
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(false)

        // Act
        handler.setPostMediaFromShareAction()

        // Assert
        verify(editorMedia, never()).addNewMediaItemsToEditorAsync(any(), any())
    }

    @Test
    fun `setPostMediaFromShareAction removes EXTRA_STREAM after processing`() {
        // Arrange
        val uri = mock<Uri>()
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(true)
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)).thenReturn(uri)
        // Mock that it's a media type
        whenever(intent.type).thenReturn("image/jpeg")

        // Act
        handler.setPostMediaFromShareAction()

        // Assert
        verify(intent).removeExtra(Intent.EXTRA_STREAM)
    }

    @Test
    fun `setPostMediaFromShareAction processes ACTION_SEND_MULTIPLE`() {
        // Arrange
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val uris = arrayListOf(uri1, uri2)
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(true)
        whenever(intent.action).thenReturn(Intent.ACTION_SEND_MULTIPLE)
        whenever(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)).thenReturn(uris)
        whenever(intent.type).thenReturn("image/*")

        // Act - method should process without throwing
        handler.setPostMediaFromShareAction()

        // Assert - verify the parcelable list was retrieved
        verify(intent).getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
    }

    @Test
    fun `setPostMediaFromShareAction processes ACTION_SEND with single image`() {
        // Arrange
        val uri = mock<Uri>()
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(true)
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.type).thenReturn("image/jpeg")
        whenever(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)).thenReturn(uri)

        // Act - method should process without throwing
        handler.setPostMediaFromShareAction()

        // Assert - verify getParcelableExtra was called for single send
        verify(intent).getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
    }

    @Test
    fun `setPostMediaFromShareAction processes ACTION_SEND with single video`() {
        // Arrange
        val uri = mock<Uri>()
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(true)
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.type).thenReturn("video/mp4")
        whenever(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)).thenReturn(uri)

        // Act - method should process without throwing
        handler.setPostMediaFromShareAction()

        // Assert - verify getParcelableExtra was called for single send
        verify(intent).getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
    }
    // endregion

    // region Edge cases
    @Test
    fun `setPostContentFromShareAction does nothing when listener returns null intent`() {
        // Arrange
        whenever(listener.getIntent()).thenReturn(null)

        // Act
        handler.setPostContentFromShareAction()

        // Assert
        verify(listener, never()).setHasSetPostContent(any())
    }

    @Test
    fun `setPostMediaFromShareAction does nothing when listener returns null intent`() {
        // Arrange
        whenever(listener.getIntent()).thenReturn(null)

        // Act
        handler.setPostMediaFromShareAction()

        // Assert
        verify(editorMedia, never()).addNewMediaItemsToEditorAsync(any(), any())
    }
    // endregion
}
