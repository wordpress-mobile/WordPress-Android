package org.wordpress.android.ui.posts.editor

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
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
import org.wordpress.android.editor.MediaToolbarAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.GifMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalUri
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.ChooserContext
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.OpenCameraForPhotos
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.OpenSystemPicker
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.SwitchMediaPicker
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import org.wordpress.android.util.UriWrapper

@RunWith(MockitoJUnitRunner::class)
class EditorPhotoPickerTest {
    @Mock
    lateinit var activity: AppCompatActivity

    @Mock
    lateinit var editorPhotoPickerListener: EditorPhotoPickerListener

    @Mock
    lateinit var editorMediaActions: EditorMediaActions

    @Mock
    lateinit var editorMedia: EditorMedia

    @Mock
    lateinit var mediaPickerLauncher: MediaPickerLauncher

    private lateinit var siteModel: SiteModel
    private lateinit var editorPhotoPicker: EditorPhotoPicker

    @Before
    fun setUp() {
        siteModel = SiteModel().apply {
            id = 1
            siteId = 100L
        }

        editorPhotoPicker = EditorPhotoPicker(
            activity = activity,
            editorPhotoPickerListener = editorPhotoPickerListener,
            editorMediaActions = editorMediaActions,
            editorMedia = editorMedia,
            mediaPickerLauncher = mediaPickerLauncher,
            siteModelProvider = { siteModel },
            showAztecEditor = false
        )
    }

    // region allowMultipleSelection tests

    @Test
    fun `allowMultipleSelection defaults to false`() {
        assertThat(editorPhotoPicker.allowMultipleSelection).isFalse()
    }

    @Test
    fun `setAllowMultipleSelection updates the value`() {
        // Act
        editorPhotoPicker.setAllowMultipleSelection(true)

        // Assert
        assertThat(editorPhotoPicker.allowMultipleSelection).isTrue()
    }

    @Test
    fun `setAllowMultipleSelection can reset to false`() {
        // Arrange
        editorPhotoPicker.setAllowMultipleSelection(true)

        // Act
        editorPhotoPicker.setAllowMultipleSelection(false)

        // Assert
        assertThat(editorPhotoPicker.allowMultipleSelection).isFalse()
    }

    // endregion

    // region isPhotoPickerShowing tests

    @Test
    fun `isPhotoPickerShowing returns false when container is null`() {
        // The container is null by default since showPhotoPicker hasn't been called
        assertThat(editorPhotoPicker.isPhotoPickerShowing()).isFalse()
    }

    // endregion

    // region onItemsChosen tests

    @Test
    fun `onItemsChosen notifies listener that picker is hidden`() {
        // Arrange
        val identifiers = emptyList<Identifier>()

        // Act
        editorPhotoPicker.onItemsChosen(identifiers)

        // Assert
        verify(editorPhotoPickerListener).onPhotoPickerHidden()
    }

    @Test
    fun `onItemsChosen adds LocalUri items to editor`() {
        // Arrange
        val uri = mock<Uri>()
        val uriWrapper = UriWrapper(uri)
        val localUri = LocalUri(uriWrapper)
        val identifiers = listOf<Identifier>(localUri)

        // Act
        editorPhotoPicker.onItemsChosen(identifiers)

        // Assert
        verify(editorMedia).addNewMediaItemsToEditorAsync(eq(listOf(uri)), eq(false))
    }

    @Test
    fun `onItemsChosen adds GifMediaIdentifier items to editor`() {
        // Arrange
        val uri = mock<Uri>()
        val uriWrapper = UriWrapper(uri)
        val gifIdentifier = GifMediaIdentifier(uriWrapper, "title")
        val identifiers = listOf<Identifier>(gifIdentifier)

        // Act
        editorPhotoPicker.onItemsChosen(identifiers)

        // Assert
        verify(editorMedia).addNewMediaItemsToEditorAsync(eq(listOf(uri)), eq(false))
    }

    @Test
    fun `onItemsChosen does not add media when list is empty`() {
        // Arrange
        val identifiers = emptyList<Identifier>()

        // Act
        editorPhotoPicker.onItemsChosen(identifiers)

        // Assert
        verify(editorMedia, never()).addNewMediaItemsToEditorAsync(any(), any())
    }

    @Test
    fun `onItemsChosen filters out unsupported identifier types`() {
        // Arrange - using a remote media id which should be filtered out
        val remoteId = Identifier.RemoteId(123L)
        val identifiers = listOf<Identifier>(remoteId)

        // Act
        editorPhotoPicker.onItemsChosen(identifiers)

        // Assert
        verify(editorMedia, never()).addNewMediaItemsToEditorAsync(any(), any())
    }

    @Test
    fun `onItemsChosen handles mixed identifier types`() {
        // Arrange
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val localUri = LocalUri(UriWrapper(uri1))
        val gifIdentifier = GifMediaIdentifier(UriWrapper(uri2), "title")
        val remoteId = Identifier.RemoteId(123L)
        val identifiers = listOf(localUri, gifIdentifier, remoteId)

        // Act
        editorPhotoPicker.onItemsChosen(identifiers)

        // Assert
        verify(editorMedia).addNewMediaItemsToEditorAsync(eq(listOf(uri1, uri2)), eq(false))
    }

    // endregion

    // region onIconClicked tests

    @Test
    fun `onIconClicked notifies listener that picker is hidden`() {
        // Arrange
        setupSiteWithUploadPermission()
        val action = OpenCameraForPhotos

        // Act
        editorPhotoPicker.onIconClicked(action)

        // Assert
        verify(editorPhotoPickerListener).onPhotoPickerHidden()
    }

    @Test
    fun `onIconClicked with OpenCameraForPhotos launches camera when user can upload`() {
        // Arrange
        setupSiteWithUploadPermission()
        val action = OpenCameraForPhotos

        // Act
        editorPhotoPicker.onIconClicked(action)

        // Assert
        verify(editorMediaActions).launchCamera()
    }

    @Test
    fun `onIconClicked with OpenSystemPicker sets allowMultipleSelection`() {
        // Arrange
        setupSiteWithUploadPermission()
        val action = OpenSystemPicker(
            chooserContext = ChooserContext.PHOTO_OR_VIDEO,
            mimeTypes = listOf("image/*", "video/*"),
            allowMultipleSelection = true
        )

        // Act
        editorPhotoPicker.onIconClicked(action)

        // Assert
        assertThat(editorPhotoPicker.allowMultipleSelection).isTrue()
    }

    @Test
    fun `onIconClicked with SwitchMediaPicker to WP_LIBRARY launches WP media picker`() {
        // Arrange
        val setup = MediaPickerSetup(
            primaryDataSource = DataSource.WP_LIBRARY,
            availableDataSources = emptySet(),
            canMultiselect = true,
            requiresPhotosVideosPermissions = false,
            requiresMusicAudioPermissions = false,
            allowedTypes = emptySet(),
            cameraSetup = MediaPickerSetup.CameraSetup.HIDDEN,
            systemPickerEnabled = false,
            editingEnabled = false,
            queueResults = false,
            defaultSearchView = false,
            title = 0
        )
        val action = SwitchMediaPicker(setup)

        // Act
        editorPhotoPicker.onIconClicked(action)

        // Assert
        verify(mediaPickerLauncher).viewWPMediaLibraryPickerForResult(
            any(), any(), any(), any()
        )
    }

    @Test
    fun `onIconClicked with SwitchMediaPicker to STOCK_LIBRARY launches stock picker`() {
        // Arrange
        val setup = MediaPickerSetup(
            primaryDataSource = DataSource.STOCK_LIBRARY,
            availableDataSources = emptySet(),
            canMultiselect = true,
            requiresPhotosVideosPermissions = false,
            requiresMusicAudioPermissions = false,
            allowedTypes = emptySet(),
            cameraSetup = MediaPickerSetup.CameraSetup.HIDDEN,
            systemPickerEnabled = false,
            editingEnabled = false,
            queueResults = false,
            defaultSearchView = false,
            title = 0
        )
        val action = SwitchMediaPicker(setup)

        // Act
        editorPhotoPicker.onIconClicked(action)

        // Assert
        verify(mediaPickerLauncher).showStockMediaPickerForResult(any(), any(), any(), eq(true))
    }

    @Test
    fun `onIconClicked with SwitchMediaPicker to GIF_LIBRARY launches GIF picker`() {
        // Arrange
        val setup = MediaPickerSetup(
            primaryDataSource = DataSource.GIF_LIBRARY,
            availableDataSources = emptySet(),
            canMultiselect = false,
            requiresPhotosVideosPermissions = false,
            requiresMusicAudioPermissions = false,
            allowedTypes = emptySet(),
            cameraSetup = MediaPickerSetup.CameraSetup.HIDDEN,
            systemPickerEnabled = false,
            editingEnabled = false,
            queueResults = false,
            defaultSearchView = false,
            title = 0
        )
        val action = SwitchMediaPicker(setup)

        // Act
        editorPhotoPicker.onIconClicked(action)

        // Assert
        verify(mediaPickerLauncher).showGifPickerForResult(any(), any(), eq(false))
    }

    // endregion

    // region onMediaToolbarButtonClicked tests

    @Test
    fun `onMediaToolbarButtonClicked with null does nothing`() {
        // null action is a no-op - just verify it doesn't throw
        editorPhotoPicker.onMediaToolbarButtonClicked(null)

        // Verify no interactions occurred
        verify(editorMediaActions, never()).launchCamera()
        verify(mediaPickerLauncher, never()).viewWPMediaLibraryPickerForResult(
            any(), any(), any(), any()
        )
    }

    @Test
    fun `onMediaToolbarButtonClicked with CAMERA launches camera when user can upload`() {
        // Arrange
        setupSiteWithUploadPermission()

        // Act
        editorPhotoPicker.onMediaToolbarButtonClicked(MediaToolbarAction.CAMERA)

        // Assert
        verify(editorMediaActions).checkCameraPermissionAndLaunch()
    }

    @Test
    fun `onMediaToolbarButtonClicked with CAMERA does not launch camera when user cannot upload`() {
        // Arrange - set up a WPCom site without upload permission
        setupSiteWithoutUploadPermission()

        // Act
        editorPhotoPicker.onMediaToolbarButtonClicked(MediaToolbarAction.CAMERA)

        // Assert
        verify(editorMediaActions, never()).checkCameraPermissionAndLaunch()
    }

    @Test
    fun `onMediaToolbarButtonClicked with LIBRARY launches WP media library`() {
        // Act
        editorPhotoPicker.onMediaToolbarButtonClicked(MediaToolbarAction.LIBRARY)

        // Assert
        verify(mediaPickerLauncher).viewWPMediaLibraryPickerForResult(any(), any(), any(), any())
    }

    @Test
    fun `onMediaToolbarButtonClicked with GALLERY notifies listener that picker is shown`() {
        // Arrange - set up fragment manager mock for showPhotoPicker
        val fragmentManager = mock<androidx.fragment.app.FragmentManager>()
        val fragmentTransaction = mock<androidx.fragment.app.FragmentTransaction>()
        whenever(activity.supportFragmentManager).thenReturn(fragmentManager)
        whenever(fragmentManager.findFragmentByTag(any())).thenReturn(null)
        whenever(fragmentManager.beginTransaction()).thenReturn(fragmentTransaction)
        whenever(fragmentTransaction.add(any<Int>(), any<androidx.fragment.app.Fragment>(), any()))
            .thenReturn(fragmentTransaction)

        // Act
        editorPhotoPicker.onMediaToolbarButtonClicked(MediaToolbarAction.GALLERY)

        // Assert
        verify(editorPhotoPickerListener).onPhotoPickerShown()
    }

    // endregion

    // region Helper methods

    private fun setupSiteWithUploadPermission() {
        // Set up a site where user can upload media
        siteModel.hasCapabilityUploadFiles = true
    }

    private fun setupSiteWithoutUploadPermission() {
        // Set up a WPCom site without upload permission
        // (self-hosted sites always allow uploads, so we need a WPCom site to test no permission)
        siteModel.setIsWPCom(true)
        siteModel.hasCapabilityUploadFiles = false
    }

    // endregion
}
