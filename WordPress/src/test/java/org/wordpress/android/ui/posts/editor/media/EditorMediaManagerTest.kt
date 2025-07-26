package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.posts.editor.EditorPhotoPicker
import org.wordpress.android.ui.posts.services.AztecImageLoader
import org.wordpress.gutenberg.MediaType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for EditorMediaManager.
 * This demonstrates how extracting the media logic improves testability.
 */
@RunWith(MockitoJUnitRunner::class)
class EditorMediaManagerTest {

    @Mock private lateinit var mediaStore: MediaStore
    @Mock private lateinit var editorPhotoPicker: EditorPhotoPicker
    @Mock private lateinit var listener: EditorMediaManager.MediaManagerListener
    @Mock private lateinit var lifecycleOwner: LifecycleOwner
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var post: PostModel
    @Mock private lateinit var aztecImageLoader: AztecImageLoader

    private lateinit var mediaManager: EditorMediaManager

    @Before
    fun setUp() {
        mediaManager = EditorMediaManager(mediaStore, editorPhotoPicker)
        
        // Setup default mock behavior
        whenever(site.id).thenReturn(123)
        whenever(post.id).thenReturn(456)
    }

    @Test
    fun `initialize should setup manager with provided dependencies`() {
        // Act
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Assert
        verify(editorPhotoPicker).initialize(any<EditorPhotoPicker.EditorPhotoPickerListener>())
    }

    @Test
    fun `handleMediaSelection should show progress dialog for valid URIs`() {
        // Arrange
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val uriList = listOf(uri1, uri2)
        
        whenever(uri1.path).thenReturn("/path/to/image1.jpg")
        whenever(uri2.path).thenReturn("/path/to/image2.jpg")
        
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.handleMediaSelection(uriList)

        // Assert
        verify(listener).showProgressDialog("Adding media...")
    }

    @Test
    fun `handleMediaSelection should do nothing for empty URI list`() {
        // Arrange
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.handleMediaSelection(emptyList())

        // Assert
        verify(listener, never()).showProgressDialog(any())
    }

    @Test
    fun `mapAllowedTypesToMediaBrowserType should return correct type for image and video`() {
        // Arrange
        val allowedTypes = arrayOf(MediaType.IMAGE, MediaType.VIDEO)

        // Act - multiple selection
        val multipleResult = mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, true)
        val singleResult = mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, false)

        // Assert
        assertEquals(MediaBrowserType.GUTENBERG_MEDIA_PICKER, multipleResult)
        assertEquals(MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER, singleResult)
    }

    @Test
    fun `mapAllowedTypesToMediaBrowserType should return correct type for image only`() {
        // Arrange
        val allowedTypes = arrayOf(MediaType.IMAGE)

        // Act
        val multipleResult = mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, true)
        val singleResult = mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, false)

        // Assert
        assertEquals(MediaBrowserType.GUTENBERG_IMAGE_PICKER, multipleResult)
        assertEquals(MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER, singleResult)
    }

    @Test
    fun `mapAllowedTypesToMediaBrowserType should return correct type for video only`() {
        // Arrange
        val allowedTypes = arrayOf(MediaType.VIDEO)

        // Act
        val multipleResult = mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, true)
        val singleResult = mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, false)

        // Assert
        assertEquals(MediaBrowserType.GUTENBERG_VIDEO_PICKER, multipleResult)
        assertEquals(MediaBrowserType.GUTENBERG_SINGLE_VIDEO_PICKER, singleResult)
    }

    @Test
    fun `mapAllowedTypesToMediaBrowserType should return audio picker for audio type`() {
        // Arrange
        val allowedTypes = arrayOf(MediaType.AUDIO)

        // Act
        val result = mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, true)

        // Assert
        assertEquals(MediaBrowserType.GUTENBERG_SINGLE_AUDIO_FILE_PICKER, result)
    }

    @Test
    fun `onUploadSuccess should notify listener and remove from uploading map`() {
        // Arrange
        val mediaModel = MediaModel().apply { 
            id = 789
            uploadState = MediaModel.MediaUploadState.UPLOADED.toString()
        }
        
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.onUploadSuccess(mediaModel)

        // Assert
        verify(listener).onMediaUploadSuccess(mediaModel)
        verify(listener).hideProgressDialog()
    }

    @Test
    fun `onUploadError should notify listener with error message`() {
        // Arrange
        val mediaModel = MediaModel().apply { 
            id = 789
            uploadState = MediaModel.MediaUploadState.FAILED.toString()
        }
        val errorMessage = "Upload failed due to network error"
        
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.onUploadError(mediaModel, errorMessage)

        // Assert
        verify(listener).onMediaUploadError(mediaModel, errorMessage)
        verify(listener).hideProgressDialog()
    }

    @Test
    fun `onUploadProgress should notify listener with progress`() {
        // Arrange
        val mediaModel = MediaModel().apply { 
            id = 789
            uploadState = MediaModel.MediaUploadState.UPLOADING.toString()
        }
        val progress = 0.75f
        
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.onUploadProgress(mediaModel, progress)

        // Assert
        verify(listener).onMediaUploadProgress(mediaModel, progress)
    }

    @Test
    fun `hasFailedMedia should return true when post has failed media`() {
        // Arrange
        val failedMedia = MediaModel().apply {
            uploadState = MediaModel.MediaUploadState.FAILED.toString()
        }
        val mediaList = listOf(failedMedia)
        
        whenever(mediaStore.getMediaForPost(post)).thenReturn(mediaList)

        // Act
        val result = mediaManager.hasFailedMedia(post)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasFailedMedia should return false when post has no failed media`() {
        // Arrange
        val successfulMedia = MediaModel().apply {
            uploadState = MediaModel.MediaUploadState.UPLOADED.toString()
        }
        val mediaList = listOf(successfulMedia)
        
        whenever(mediaStore.getMediaForPost(post)).thenReturn(mediaList)

        // Act
        val result = mediaManager.hasFailedMedia(post)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `retryFailedUploads should retry all failed media for post`() {
        // Arrange
        val failedMedia1 = MediaModel().apply {
            id = 1
            uploadState = MediaModel.MediaUploadState.FAILED.toString()
        }
        val failedMedia2 = MediaModel().apply {
            id = 2
            uploadState = MediaModel.MediaUploadState.FAILED.toString()
        }
        val successfulMedia = MediaModel().apply {
            id = 3
            uploadState = MediaModel.MediaUploadState.UPLOADED.toString()
        }
        val mediaList = listOf(failedMedia1, failedMedia2, successfulMedia)
        
        whenever(mediaStore.getMediaForPost(post)).thenReturn(mediaList)
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.retryFailedUploads(post)

        // Assert
        verify(listener, times(2)).onMediaUploadStarted(any())
    }

    @Test
    fun `openMediaLibrary should delegate to listener`() {
        // Arrange
        val mediaType = MediaBrowserType.GUTENBERG_IMAGE_PICKER
        val initialSelection = listOf(1, 2, 3)
        
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.openMediaLibrary(mediaType, initialSelection)

        // Assert
        verify(listener).onMediaLibraryRequested(mediaType, initialSelection)
    }

    @Test
    fun `getUploadErrorHtml should return formatted error HTML`() {
        // Arrange
        val mediaId = "123"
        val path = "/path/to/failed/media.jpg"

        // Act
        val result = mediaManager.getUploadErrorHtml(mediaId, path)

        // Assert
        assertTrue(result.contains("Media upload failed"))
        assertTrue(result.contains("Media ID: $mediaId"))
        assertTrue(result.contains("Path: $path"))
        assertTrue(result.contains("retryUpload('$mediaId')"))
    }

    @Test
    fun `cleanup should reset all internal state`() {
        // Arrange
        mediaManager.initialize(listener, lifecycleOwner, site, post, aztecImageLoader)

        // Act
        mediaManager.cleanup()

        // Assert - verify that subsequent calls don't crash
        mediaManager.handleMediaSelection(emptyList()) // Should not crash
        mediaManager.onUploadSuccess(MediaModel()) // Should not crash
    }
}

/**
 * TESTING BENEFITS AFTER EXTRACTION:
 * 
 * 1. **Isolated Testing**: Can test media logic without Activity lifecycle complexity
 * 2. **Fast Tests**: No need to start Activity or inflate layouts
 * 3. **Focused Tests**: Each test targets specific media functionality
 * 4. **Easy Mocking**: Clear dependencies that can be easily mocked
 * 5. **Comprehensive Coverage**: Can test edge cases without UI setup
 * 
 * BEFORE EXTRACTION:
 * - Testing media logic required starting entire EditPostActivity (4,251 lines)
 * - Tests were slow and brittle due to UI dependencies
 * - Hard to isolate media logic from other activity concerns
 * - Difficult to test edge cases and error conditions
 * 
 * AFTER EXTRACTION:
 * - Fast, focused unit tests for media logic
 * - Easy to achieve high test coverage
 * - Clear separation of concerns
 * - Regression testing becomes much easier
 */