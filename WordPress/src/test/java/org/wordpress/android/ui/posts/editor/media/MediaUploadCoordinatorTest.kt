package org.wordpress.android.ui.posts.editor.media

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class MediaUploadCoordinatorTest {
    @Mock
    private lateinit var editorMedia: EditorMedia

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var listener: MediaUploadCoordinator.UploadEventListener

    @Mock
    private lateinit var editorMediaUploadListener: EditorMediaUploadListener

    private lateinit var coordinator: MediaUploadCoordinator

    @Before
    fun setUp() {
        coordinator = MediaUploadCoordinator(editorMedia, networkUtilsWrapper)
        whenever(listener.isFinishing()).thenReturn(false)
        whenever(listener.getPostId()).thenReturn(TEST_POST_ID)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        coordinator.start(listener, editorMediaUploadListener)
    }

    // region processMediaUploadedEvent tests
    @Test
    fun `processMediaUploadedEvent returns false when Activity is finishing`() {
        // Arrange
        whenever(listener.isFinishing()).thenReturn(true)
        val event = createMediaUploadedEvent(isError = false, completed = true)

        // Act
        val result = coordinator.processMediaUploadedEvent(event)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `processMediaUploadedEvent pauses upload when error and no network`() {
        // Arrange
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        val media = createMediaModel()
        val error = MediaError(MediaErrorType.GENERIC_ERROR)
        val event = createMediaUploadedEvent(media = media, isError = true, error = error)

        // Act
        val result = coordinator.processMediaUploadedEvent(event)

        // Assert
        assertThat(result).isTrue()
        verify(editorMedia).onMediaUploadPaused(eq(editorMediaUploadListener), eq(media), eq(error))
    }

    @Test
    fun `processMediaUploadedEvent handles error when network available`() {
        // Arrange
        val media = createMediaModel()
        val error = MediaError(MediaErrorType.GENERIC_ERROR)
        val event = createMediaUploadedEvent(media = media, isError = true, error = error)

        // Act
        val result = coordinator.processMediaUploadedEvent(event)

        // Assert
        assertThat(result).isTrue()
        verify(editorMedia).onMediaUploadError(eq(editorMediaUploadListener), eq(media), eq(error))
    }

    @Test
    fun `processMediaUploadedEvent handles successful completion with valid URL`() {
        // Arrange
        val media = createMediaModel(url = "https://example.com/image.jpg")
        val event = createMediaUploadedEvent(media = media, completed = true)

        // Act
        val result = coordinator.processMediaUploadedEvent(event)

        // Assert
        assertThat(result).isTrue()
        verify(editorMediaUploadListener).onMediaUploadSucceeded(eq(media.id.toString()), any())
    }

    @Test
    fun `processMediaUploadedEvent handles completion with empty URL as error`() {
        // Arrange
        val media = createMediaModel(url = "")
        val event = createMediaUploadedEvent(media = media, completed = true)

        // Act
        val result = coordinator.processMediaUploadedEvent(event)

        // Assert
        assertThat(result).isTrue()
        verify(editorMedia).onMediaUploadError(eq(editorMediaUploadListener), eq(media), any())
    }

    @Test
    fun `processMediaUploadedEvent handles progress update`() {
        // Arrange
        val media = createMediaModel()
        val event = createMediaUploadedEvent(media = media, progress = 0.5f)

        // Act
        val result = coordinator.processMediaUploadedEvent(event)

        // Assert
        assertThat(result).isTrue()
        verify(editorMediaUploadListener).onMediaUploadProgress(eq(media.id.toString()), eq(0.5f))
    }

    @Test
    fun `processMediaUploadedEvent returns false when media is null`() {
        // Arrange
        val event = createMediaUploadedEvent(media = null)

        // Act
        val result = coordinator.processMediaUploadedEvent(event)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `processMediaUploadedEvent calls onFeaturedImageUploaded for featured image`() {
        // Arrange
        val media = createMediaModel(
            url = "https://example.com/image.jpg",
            markedLocallyAsFeatured = true,
            localPostId = TEST_POST_ID,
            mediaId = TEST_MEDIA_ID
        )
        val event = createMediaUploadedEvent(media = media, completed = true)

        // Act
        coordinator.processMediaUploadedEvent(event)

        // Assert
        verify(listener).onFeaturedImageUploaded(TEST_MEDIA_ID)
        verify(editorMediaUploadListener, never()).onMediaUploadSucceeded(any(), any())
    }
    // endregion

    // region handleUploadSuccess tests
    @Test
    fun `handleUploadSuccess notifies listener for non-featured image`() {
        // Arrange
        val media = createMediaModel(
            markedLocallyAsFeatured = false,
            url = "https://example.com/image.jpg"
        )

        // Act
        coordinator.handleUploadSuccess(media)

        // Assert
        verify(editorMediaUploadListener).onMediaUploadSucceeded(eq(media.id.toString()), any())
    }

    @Test
    fun `handleUploadSuccess calls onFeaturedImageUploaded for featured image in current post`() {
        // Arrange
        val media = createMediaModel(
            markedLocallyAsFeatured = true,
            localPostId = TEST_POST_ID,
            mediaId = TEST_MEDIA_ID
        )

        // Act
        coordinator.handleUploadSuccess(media)

        // Assert
        verify(listener).onFeaturedImageUploaded(TEST_MEDIA_ID)
    }

    @Test
    fun `handleUploadSuccess ignores featured image for different post`() {
        // Arrange
        val media = createMediaModel(
            markedLocallyAsFeatured = true,
            localPostId = 999, // Different post ID
            mediaId = TEST_MEDIA_ID
        )

        // Act
        coordinator.handleUploadSuccess(media)

        // Assert
        verify(listener, never()).onFeaturedImageUploaded(any())
        verify(editorMediaUploadListener, never()).onMediaUploadSucceeded(any(), any())
    }
    // endregion

    // region handleUploadProgress tests
    @Test
    fun `handleUploadProgress notifies editor listener`() {
        // Arrange
        val media = createMediaModel()

        // Act
        coordinator.handleUploadProgress(media, 0.75f)

        // Assert
        verify(editorMediaUploadListener).onMediaUploadProgress(eq(media.id.toString()), eq(0.75f))
    }

    @Test
    fun `handleUploadProgress handles null media`() {
        // Act
        coordinator.handleUploadProgress(null, 0.5f)

        // Assert
        verify(editorMediaUploadListener).onMediaUploadProgress(eq("null"), eq(0.5f))
    }
    // endregion

    // region setEditorMediaUploadListener tests
    @Test
    fun `setEditorMediaUploadListener updates listener for subsequent events`() {
        // Arrange
        val newListener = org.mockito.kotlin.mock<EditorMediaUploadListener>()
        coordinator.setEditorMediaUploadListener(newListener)
        val media = createMediaModel()

        // Act
        coordinator.handleUploadProgress(media, 0.5f)

        // Assert
        verify(newListener).onMediaUploadProgress(any(), any())
        verify(editorMediaUploadListener, never()).onMediaUploadProgress(any(), any())
    }
    // endregion

    // region Helper methods
    private fun createMediaModel(
        id: Int = TEST_MEDIA_LOCAL_ID,
        mediaId: Long = TEST_MEDIA_ID,
        url: String? = "https://example.com/image.jpg",
        markedLocallyAsFeatured: Boolean = false,
        localPostId: Int = TEST_POST_ID,
        isVideo: Boolean = false
    ): MediaModel {
        return MediaModel(0, 0).apply {
            this.id = id
            this.mediaId = mediaId
            url?.let { setUrl(it) }
            this.markedLocallyAsFeatured = markedLocallyAsFeatured
            this.localPostId = localPostId
            // isVideo is determined by mime type
            setMimeType(if (isVideo) "video/mp4" else "image/jpeg")
        }
    }

    private fun createMediaUploadedEvent(
        media: MediaModel? = createMediaModel(),
        isError: Boolean = false,
        completed: Boolean = false,
        progress: Float = 0f,
        error: MediaError = MediaError(MediaErrorType.GENERIC_ERROR)
    ): OnMediaUploaded {
        return OnMediaUploaded(media, progress, completed, false).apply {
            if (isError) {
                this.error = error
            }
        }
    }
    // endregion

    companion object {
        private const val TEST_POST_ID = 123
        private const val TEST_MEDIA_LOCAL_ID = 456
        private const val TEST_MEDIA_ID = 789L
    }
}
