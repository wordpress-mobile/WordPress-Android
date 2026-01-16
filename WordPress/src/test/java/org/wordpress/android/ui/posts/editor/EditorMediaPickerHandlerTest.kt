package org.wordpress.android.ui.posts.editor

import android.app.Activity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.EditorCameraHelper
import org.wordpress.android.ui.RequestCodes

@RunWith(MockitoJUnitRunner::class)
class EditorMediaPickerHandlerTest {
    @Mock
    private lateinit var mediaPickerLauncher: MediaPickerLauncher

    @Mock
    private lateinit var editorCameraHelper: EditorCameraHelper

    @Mock
    private lateinit var listener: EditorMediaPickerHandler.MediaPickerListener

    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var editorPhotoPicker: EditorPhotoPicker

    private lateinit var handler: EditorMediaPickerHandler

    @Before
    fun setUp() {
        handler = EditorMediaPickerHandler(mediaPickerLauncher, editorCameraHelper)
        whenever(listener.getActivity()).thenReturn(activity)
        whenever(listener.getSiteModel()).thenReturn(siteModel)
        whenever(listener.getPostId()).thenReturn(TEST_POST_ID)
        whenever(listener.getEditorPhotoPicker()).thenReturn(editorPhotoPicker)
        handler.start(listener)
    }

    // region onAddMediaClicked tests
    @Test
    fun `onAddMediaClicked hides picker when already showing`() {
        // Arrange
        whenever(editorPhotoPicker.isPhotoPickerShowing()).thenReturn(true)

        // Act
        handler.onAddMediaClicked()

        // Assert
        verify(editorPhotoPicker).hidePhotoPicker()
    }

    @Test
    fun `onAddMediaClicked attempts to show picker when not already showing`() {
        // Arrange
        whenever(editorPhotoPicker.isPhotoPickerShowing()).thenReturn(false)

        // Act - method should process without throwing
        // Note: The actual behavior depends on WPMediaUtils.currentUserCanUploadMedia which
        // requires static mocking. This test verifies the method handles the case gracefully.
        handler.onAddMediaClicked()

        // Assert - verify isPhotoPickerShowing was checked
        verify(editorPhotoPicker).isPhotoPickerShowing()
    }
    // endregion

    // region onAddMediaImageClicked tests
    @Test
    fun `onAddMediaImageClicked launches WP media library with image picker`() {
        // Arrange
        val allowMultipleSelection = true

        // Act
        handler.onAddMediaImageClicked(allowMultipleSelection)

        // Assert
        verify(editorPhotoPicker).setAllowMultipleSelection(allowMultipleSelection)
        verify(mediaPickerLauncher).viewWPMediaLibraryPickerForResult(
            activity,
            siteModel,
            MediaBrowserType.GUTENBERG_IMAGE_PICKER
        )
    }

    @Test
    fun `onAddMediaImageClicked returns early when listener returns null activity`() {
        // Arrange - create a new handler with a listener that returns null activity
        val nullActivityListener = mock<EditorMediaPickerHandler.MediaPickerListener>()
        whenever(nullActivityListener.getActivity()).thenReturn(null)
        val testHandler = EditorMediaPickerHandler(mediaPickerLauncher, editorCameraHelper)
        testHandler.start(nullActivityListener)

        // Act - should return early without throwing
        testHandler.onAddMediaImageClicked(true)

        // Assert - no exception thrown (implicit verification that method handled null gracefully)
    }
    // endregion

    // region onAddMediaVideoClicked tests
    @Test
    fun `onAddMediaVideoClicked launches WP media library with video picker`() {
        // Arrange
        val allowMultipleSelection = false

        // Act
        handler.onAddMediaVideoClicked(allowMultipleSelection)

        // Assert
        verify(editorPhotoPicker).setAllowMultipleSelection(allowMultipleSelection)
        verify(mediaPickerLauncher).viewWPMediaLibraryPickerForResult(
            activity,
            siteModel,
            MediaBrowserType.GUTENBERG_VIDEO_PICKER
        )
    }
    // endregion

    // region onAddLibraryMediaClicked tests
    @Test
    fun `onAddLibraryMediaClicked with multiselect launches editor picker`() {
        // Act
        handler.onAddLibraryMediaClicked(allowMultipleSelection = true)

        // Assert
        verify(mediaPickerLauncher).viewWPMediaLibraryPickerForResult(
            activity,
            siteModel,
            MediaBrowserType.EDITOR_PICKER
        )
    }

    @Test
    fun `onAddLibraryMediaClicked without multiselect launches single media picker`() {
        // Act
        handler.onAddLibraryMediaClicked(allowMultipleSelection = false)

        // Assert
        verify(mediaPickerLauncher).viewWPMediaLibraryPickerForResult(
            activity,
            siteModel,
            MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER
        )
    }
    // endregion

    // region onAddPhotoClicked tests
    @Test
    fun `onAddPhotoClicked with multiselect launches image picker`() {
        // Act
        handler.onAddPhotoClicked(allowMultipleSelection = true)

        // Assert
        verify(mediaPickerLauncher).showPhotoPickerForResult(
            activity,
            MediaBrowserType.GUTENBERG_IMAGE_PICKER,
            siteModel,
            TEST_POST_ID
        )
    }

    @Test
    fun `onAddPhotoClicked without multiselect launches single image picker`() {
        // Act
        handler.onAddPhotoClicked(allowMultipleSelection = false)

        // Assert
        verify(mediaPickerLauncher).showPhotoPickerForResult(
            activity,
            MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER,
            siteModel,
            TEST_POST_ID
        )
    }
    // endregion

    // region onAddVideoClicked tests
    @Test
    fun `onAddVideoClicked with multiselect launches video picker`() {
        // Act
        handler.onAddVideoClicked(allowMultipleSelection = true)

        // Assert
        verify(mediaPickerLauncher).showPhotoPickerForResult(
            activity,
            MediaBrowserType.GUTENBERG_VIDEO_PICKER,
            siteModel,
            TEST_POST_ID
        )
    }

    @Test
    fun `onAddVideoClicked without multiselect launches single video picker`() {
        // Act
        handler.onAddVideoClicked(allowMultipleSelection = false)

        // Assert
        verify(mediaPickerLauncher).showPhotoPickerForResult(
            activity,
            MediaBrowserType.GUTENBERG_SINGLE_VIDEO_PICKER,
            siteModel,
            TEST_POST_ID
        )
    }
    // endregion

    // region onAddDeviceMediaClicked tests
    @Test
    fun `onAddDeviceMediaClicked with multiselect launches media picker`() {
        // Act
        handler.onAddDeviceMediaClicked(allowMultipleSelection = true)

        // Assert
        verify(mediaPickerLauncher).showPhotoPickerForResult(
            activity,
            MediaBrowserType.GUTENBERG_MEDIA_PICKER,
            siteModel,
            TEST_POST_ID
        )
    }

    @Test
    fun `onAddDeviceMediaClicked without multiselect launches single media picker`() {
        // Act
        handler.onAddDeviceMediaClicked(allowMultipleSelection = false)

        // Assert
        verify(mediaPickerLauncher).showPhotoPickerForResult(
            activity,
            MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER,
            siteModel,
            TEST_POST_ID
        )
    }
    // endregion

    // region onAddStockMediaClicked tests
    @Test
    fun `onAddStockMediaClicked with multiselect uses multi select request code`() {
        // Act
        handler.onAddStockMediaClicked(allowMultipleSelection = true)

        // Assert
        verify(mediaPickerLauncher).showStockMediaPickerForResult(
            activity,
            siteModel,
            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT,
            true
        )
    }

    @Test
    fun `onAddStockMediaClicked without multiselect uses single select request code`() {
        // Act
        handler.onAddStockMediaClicked(allowMultipleSelection = false)

        // Assert
        verify(mediaPickerLauncher).showStockMediaPickerForResult(
            activity,
            siteModel,
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK,
            false
        )
    }
    // endregion

    // region onAddGifClicked tests
    @Test
    fun `onAddGifClicked launches gif picker`() {
        // Act
        handler.onAddGifClicked(allowMultipleSelection = true)

        // Assert
        verify(mediaPickerLauncher).showGifPickerForResult(activity, siteModel, true)
    }
    // endregion

    // region onAddFileClicked tests
    @Test
    fun `onAddFileClicked launches file picker`() {
        // Act
        handler.onAddFileClicked(allowMultipleSelection = true)

        // Assert
        verify(mediaPickerLauncher).showFilePicker(activity, true, siteModel)
    }
    // endregion

    // region onAddAudioFileClicked tests
    @Test
    fun `onAddAudioFileClicked launches audio file picker`() {
        // Act
        handler.onAddAudioFileClicked(allowMultipleSelection = false)

        // Assert
        verify(mediaPickerLauncher).showAudioFilePicker(activity, false, siteModel)
    }
    // endregion

    // region onCapturePhotoClicked tests
    @Test
    fun `onCapturePhotoClicked delegates to editorCameraHelper`() {
        // Act
        handler.onCapturePhotoClicked()

        // Assert
        verify(editorCameraHelper).capturePhotoIfAllowed(
            org.mockito.kotlin.eq(siteModel),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }
    // endregion

    // region onCaptureVideoClicked tests
    @Test
    fun `onCaptureVideoClicked delegates to editorCameraHelper`() {
        // Act
        handler.onCaptureVideoClicked()

        // Assert
        verify(editorCameraHelper).captureVideoIfAllowed(
            org.mockito.kotlin.eq(activity),
            org.mockito.kotlin.eq(siteModel),
            org.mockito.kotlin.any()
        )
    }
    // endregion

    companion object {
        private const val TEST_POST_ID = 123
    }
}
