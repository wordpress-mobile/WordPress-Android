package org.wordpress.android.ui.posts.editor

import android.content.Intent
import android.net.Uri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaSource
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import java.util.function.Consumer

@RunWith(MockitoJUnitRunner::class)
class EditorActivityResultHandlerTest {
    @Mock
    private lateinit var editorMedia: EditorMedia

    @Mock
    private lateinit var imageEditorTracker: ImageEditorTracker

    @Mock
    private lateinit var listener: EditorActivityResultHandler.ActivityResultListener

    @Mock
    private lateinit var editorFragment: EditorFragmentAbstract

    private lateinit var handler: EditorActivityResultHandler

    @Before
    fun setUp() {
        handler = EditorActivityResultHandler(editorMedia, imageEditorTracker)
        whenever(listener.getEditorFragment()).thenReturn(editorFragment)
        handler.start(listener)
    }

    // region handleNotOKResult tests
    @Test
    fun `handleNotOKResult cancels media selection for MULTI_SELECT_MEDIA_PICKER`() {
        // Act
        val handled = handler.handleNotOKResult(RequestCodes.MULTI_SELECT_MEDIA_PICKER)

        // Assert
        assertThat(handled).isTrue()
        verify(editorFragment).mediaSelectionCancelled()
    }

    @Test
    fun `handleNotOKResult cancels media selection for SINGLE_SELECT_MEDIA_PICKER`() {
        // Act
        val handled = handler.handleNotOKResult(RequestCodes.SINGLE_SELECT_MEDIA_PICKER)

        // Assert
        assertThat(handled).isTrue()
        verify(editorFragment).mediaSelectionCancelled()
    }

    @Test
    fun `handleNotOKResult cancels media selection for PHOTO_PICKER`() {
        // Act
        val handled = handler.handleNotOKResult(RequestCodes.PHOTO_PICKER)

        // Assert
        assertThat(handled).isTrue()
        verify(editorFragment).mediaSelectionCancelled()
    }

    @Test
    fun `handleNotOKResult cancels media selection for TAKE_PHOTO`() {
        // Act
        val handled = handler.handleNotOKResult(RequestCodes.TAKE_PHOTO)

        // Assert
        assertThat(handled).isTrue()
        verify(editorFragment).mediaSelectionCancelled()
    }

    @Test
    fun `handleNotOKResult cancels media selection for TAKE_VIDEO`() {
        // Act
        val handled = handler.handleNotOKResult(RequestCodes.TAKE_VIDEO)

        // Assert
        assertThat(handled).isTrue()
        verify(editorFragment).mediaSelectionCancelled()
    }

    @Test
    fun `handleNotOKResult returns false for unknown request code`() {
        // Act
        val handled = handler.handleNotOKResult(999)

        // Assert
        assertThat(handled).isFalse()
        verify(editorFragment, never()).mediaSelectionCancelled()
    }
    // endregion

    // region handleOKResult tests
    @Test
    fun `handleOKResult handles MULTI_SELECT_MEDIA_PICKER`() {
        // Arrange
        val intent = createIntentWithMediaIds(longArrayOf(1L, 2L, 3L))

        // Act
        val handled = handler.handleOKResult(RequestCodes.MULTI_SELECT_MEDIA_PICKER, intent)

        // Assert
        assertThat(handled).isTrue()
        verify(editorMedia).addExistingMediaToEditorAsync(
            eq(AddExistingMediaSource.WP_MEDIA_LIBRARY),
            eq(longArrayOf(1L, 2L, 3L))
        )
    }

    @Test
    fun `handleOKResult handles SINGLE_SELECT_MEDIA_PICKER`() {
        // Arrange
        val intent = createIntentWithMediaIds(longArrayOf(1L))

        // Act
        val handled = handler.handleOKResult(RequestCodes.SINGLE_SELECT_MEDIA_PICKER, intent)

        // Assert
        assertThat(handled).isTrue()
        verify(editorMedia).addExistingMediaToEditorAsync(
            eq(AddExistingMediaSource.WP_MEDIA_LIBRARY),
            eq(longArrayOf(1L))
        )
    }

    @Test
    fun `handleOKResult handles TAKE_PHOTO by delegating to listener`() {
        // Act
        val handled = handler.handleOKResult(RequestCodes.TAKE_PHOTO, null)

        // Assert
        assertThat(handled).isTrue()
        verify(listener).handleLastTakenPicture()
    }

    @Test
    fun `handleOKResult handles TAKE_VIDEO with data`() {
        // Arrange
        val uri = mock<Uri>()
        val intent = mock<Intent>()
        whenever(intent.data).thenReturn(uri)

        // Act
        val handled = handler.handleOKResult(RequestCodes.TAKE_VIDEO, intent)

        // Assert
        assertThat(handled).isTrue()
        verify(editorMedia).addNewMediaToEditorAsync(uri, true)
    }

    @Test
    fun `handleOKResult handles STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK`() {
        // Arrange
        val intent = createIntentWithSingleMediaId(123L)

        // Act
        val handled = handler.handleOKResult(
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK,
            intent
        )

        // Assert
        assertThat(handled).isTrue()
        verify(editorMedia).addExistingMediaToEditorAsync(
            eq(AddExistingMediaSource.STOCK_PHOTO_LIBRARY),
            eq(longArrayOf(123L))
        )
    }

    @Test
    fun `handleOKResult handles STOCK_MEDIA_PICKER_MULTI_SELECT`() {
        // Arrange
        val intent = createIntentWithMediaIds(longArrayOf(1L, 2L))

        // Act
        val handled = handler.handleOKResult(RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT, intent)

        // Assert
        assertThat(handled).isTrue()
        verify(editorMedia).addExistingMediaToEditorAsync(
            eq(AddExistingMediaSource.STOCK_PHOTO_LIBRARY),
            eq(longArrayOf(1L, 2L))
        )
    }

    @Test
    fun `handleOKResult handles GIF_PICKER_SINGLE_SELECT`() {
        // Arrange
        val localIds = intArrayOf(1, 2, 3)
        val intent = createIntentWithGifLocalIds(localIds)

        // Act
        val handled = handler.handleOKResult(RequestCodes.GIF_PICKER_SINGLE_SELECT, intent)

        // Assert
        assertThat(handled).isTrue()
        verify(editorMedia).addGifMediaToPostAsync(localIds)
    }

    @Test
    fun `handleOKResult handles HISTORY_DETAIL when revision exists`() {
        // Arrange
        whenever(listener.getRevisionFromBundle()).thenReturn(mock<Any>())

        // Act
        val handled = handler.handleOKResult(RequestCodes.HISTORY_DETAIL, null)

        // Assert
        assertThat(handled).isTrue()
        verify(listener).navigateToEditor()
    }

    @Test
    fun `handleOKResult handles HISTORY_DETAIL when no revision`() {
        // Arrange
        whenever(listener.getRevisionFromBundle()).thenReturn(null)

        // Act
        val handled = handler.handleOKResult(RequestCodes.HISTORY_DETAIL, null)

        // Assert
        assertThat(handled).isTrue()
        verify(listener, never()).navigateToEditor()
    }

    @Test
    fun `handleOKResult handles SELECTED_USER_MENTION`() {
        // Arrange
        val callback = mock<Consumer<String?>>()
        whenever(listener.getSuggestionCallback()).thenReturn(callback)
        val intent = mock<Intent>()
        whenever(intent.getStringExtra(any())).thenReturn("@username")

        // Act
        val handled = handler.handleOKResult(RequestCodes.SELECTED_USER_MENTION, intent)

        // Assert
        assertThat(handled).isTrue()
        verify(callback).accept("@username")
        verify(listener).clearSuggestionCallback()
    }

    @Test
    fun `handleOKResult returns false for unknown request code`() {
        // Act
        val handled = handler.handleOKResult(999, null)

        // Assert
        assertThat(handled).isFalse()
    }
    // endregion

    // region Helper methods
    private fun createIntentWithMediaIds(ids: LongArray): Intent {
        val intent = mock<Intent>()
        whenever(intent.hasExtra(MediaBrowserActivity.RESULT_IDS)).thenReturn(true)
        whenever(intent.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS)).thenReturn(ids)
        return intent
    }

    private fun createIntentWithSingleMediaId(mediaId: Long): Intent {
        val intent = mock<Intent>()
        whenever(intent.hasExtra(MediaPickerConstants.EXTRA_MEDIA_ID)).thenReturn(true)
        whenever(intent.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0)).thenReturn(mediaId)
        return intent
    }

    private fun createIntentWithGifLocalIds(localIds: IntArray): Intent {
        val intent = mock<Intent>()
        whenever(intent.getIntArrayExtra(MediaPickerConstants.EXTRA_SAVED_MEDIA_MODEL_LOCAL_IDS))
            .thenReturn(localIds)
        return intent
    }
    // endregion
}
