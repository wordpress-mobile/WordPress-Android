package org.wordpress.android.ui.posts.editor

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.Orientation
import org.wordpress.android.R
import org.wordpress.android.editor.MediaToolbarAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaPickerFragment
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.widgets.WPSnackbar

private const val MEDIA_PICKER_TAG = "media_picker"

interface EditorPhotoPickerListener {
    fun onPhotoPickerShown()
    fun onPhotoPickerHidden()
}

/**
 * Callback interface for activity-specific media actions that cannot be handled
 * generically by EditorPhotoPicker.
 */
interface EditorMediaActions {
    fun launchCamera()

    /**
     * Checks for camera permissions and launches the camera if granted.
     * If permissions are not granted, requests them from the user.
     * The activity should handle the permission result and call launchCamera() when granted.
     */
    fun checkCameraPermissionAndLaunch()
}

/**
 * This class is extracted from EditPostActivity as part of a huge refactor. It manages the embedded
 * photo picker fragment and handles media picker listener callbacks, delegating activity-specific
 * actions through [EditorMediaActions].
 */
@Suppress("LongParameterList")
class EditorPhotoPicker(
    private val activity: AppCompatActivity,
    private val editorPhotoPickerListener: EditorPhotoPickerListener,
    private val editorMediaActions: EditorMediaActions,
    private val editorMedia: EditorMedia,
    private val mediaPickerLauncher: MediaPickerLauncher,
    private val siteModelProvider: () -> SiteModel,
    private val showAztecEditor: Boolean
) : MediaToolbarAction.MediaToolbarButtonClickListener, MediaPickerFragment.MediaPickerListener {
    private var photoPickerContainer: View? = null

    private var mediaPickerFragment: MediaPickerFragment? = null
    private var photoPickerOrientation = Configuration.ORIENTATION_UNDEFINED
    var allowMultipleSelection: Boolean = false
        private set

    fun setAllowMultipleSelection(value: Boolean) {
        allowMultipleSelection = value
    }

    /*
     * loads the media picker fragment, which is hidden until the user taps the media icon
     */
    private fun initPhotoPicker(site: SiteModel) {
        photoPickerContainer = activity.findViewById(R.id.photo_fragment_container)

        // size the picker before creating the fragment to avoid having it load media now
        resizePhotoPicker()

        mediaPickerFragment = activity.supportFragmentManager.findFragmentByTag(MEDIA_PICKER_TAG)
                as? MediaPickerFragment
        if (mediaPickerFragment == null) {
            val mediaPickerSetup = buildEditorMediaPickerSetup(site)

            MediaPickerFragment.newInstance(
                this,
                mediaPickerSetup,
                site
            ).let {
                mediaPickerFragment = it
                activity.supportFragmentManager
                    .beginTransaction()
                    .add(R.id.photo_fragment_container, it, MEDIA_PICKER_TAG)
                    .commit()
            }
        }
    }

    /**
     * Builds a MediaPickerSetup configured for the editor use case.
     */
    private fun buildEditorMediaPickerSetup(site: SiteModel?): MediaPickerSetup {
        val availableDataSources = mutableSetOf<DataSource>()
        if (site != null) {
            availableDataSources.add(DataSource.WP_LIBRARY)
            if (site.isUsingWpComRestApi) {
                availableDataSources.add(DataSource.STOCK_LIBRARY)
            }
            availableDataSources.add(DataSource.GIF_LIBRARY)
        }

        return MediaPickerSetup(
            primaryDataSource = DataSource.DEVICE,
            availableDataSources = availableDataSources,
            canMultiselect = true,
            requiresPhotosVideosPermissions = true,
            requiresMusicAudioPermissions = false,
            allowedTypes = setOf(MediaType.IMAGE, MediaType.VIDEO),
            cameraSetup = if (showAztecEditor) CameraSetup.HIDDEN else CameraSetup.ENABLED,
            systemPickerEnabled = true,
            editingEnabled = true,
            queueResults = false,
            defaultSearchView = false,
            title = R.string.photo_picker_photo_or_video_title
        )
    }

    fun isPhotoPickerShowing(): Boolean {
        return photoPickerContainer?.visibility == View.VISIBLE
    }

    /*
     * user has requested to show the photo picker
     */
    fun showPhotoPicker(site: SiteModel) {
        val isAlreadyShowing = isPhotoPickerShowing()

        // make sure we initialized the photo picker
        if (mediaPickerFragment == null) {
            initPhotoPicker(site)
        }

        // hide soft keyboard
        ActivityUtils.hideKeyboard(activity)

        // slide in the photo picker
        if (!isAlreadyShowing) {
            AniUtils.animateBottomBar(photoPickerContainer, true, AniUtils.Duration.MEDIUM)
            mediaPickerFragment?.refresh()
            mediaPickerFragment?.setMediaPickerListener(this)
        }

        editorPhotoPickerListener.onPhotoPickerShown()
    }

    fun hidePhotoPicker() {
        if (isPhotoPickerShowing()) {
            mediaPickerFragment?.clearSelection()
            mediaPickerFragment?.setMediaPickerListener(null)
            AniUtils.animateBottomBar(photoPickerContainer, false)
        }
        editorPhotoPickerListener.onPhotoPickerHidden()
    }

    /*
     * resizes the photo picker based on device orientation - full height in landscape, half
     * height in portrait
     */
    private fun resizePhotoPicker() {
        if (photoPickerContainer == null) {
            return
        }

        val updatePickerContainerHeight = { newHeight: Int ->
            photoPickerContainer?.let {
                it.layoutParams.height = newHeight
            }
        }

        if (DisplayUtils.isLandscape(activity)) {
            photoPickerOrientation = Configuration.ORIENTATION_LANDSCAPE
            updatePickerContainerHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            photoPickerOrientation = Configuration.ORIENTATION_PORTRAIT
            val displayHeight = DisplayUtils.getWindowPixelHeight(activity)
            updatePickerContainerHeight((displayHeight * PORTRAIT_HEIGHT_RATIO).toInt())
        }
    }

    override fun onMediaToolbarButtonClicked(action: MediaToolbarAction?) {
        val siteModel = siteModelProvider()
        when (action) {
            MediaToolbarAction.GALLERY -> {
                // Show the embedded photo picker for selecting media from device
                showPhotoPicker(siteModel)
            }
            MediaToolbarAction.CAMERA -> {
                // Launch the camera to capture a photo (after checking permissions)
                if (WPMediaUtils.currentUserCanUploadMedia(siteModel)) {
                    editorMediaActions.checkCameraPermissionAndLaunch()
                } else {
                    showNoUploadPermissionSnackbar()
                }
            }
            MediaToolbarAction.LIBRARY -> {
                // Open the WP Media Library
                mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
                    activity,
                    siteModel,
                    MediaBrowserType.EDITOR_PICKER
                )
            }
            null -> { /* no-op */ }
        }
    }

    fun onOrientationChanged(@Orientation newOrientation: Int) {
        // resize the photo picker if the user rotated the device
        if (newOrientation != photoPickerOrientation) {
            resizePhotoPicker()
        }
    }

    // region MediaPickerListener implementation

    override fun onItemsChosen(identifiers: List<Identifier>) {
        hidePhotoPicker()
        val uriList = identifiers.mapNotNull { identifier ->
            when (identifier) {
                is Identifier.LocalUri -> identifier.value.uri
                is Identifier.GifMediaIdentifier -> identifier.largeImageUri.uri
                else -> null
            }
        }
        if (uriList.isNotEmpty()) {
            editorMedia.addNewMediaItemsToEditorAsync(uriList, false)
        }
    }

    /**
     * Called by MediaPickerFragment when user clicks an icon to launch the camera, native
     * picker, or WP media picker.
     */
    override fun onIconClicked(action: MediaPickerAction) {
        hidePhotoPicker()
        val siteModel = siteModelProvider()
        when (action) {
            is MediaPickerAction.OpenCameraForPhotos -> {
                if (WPMediaUtils.currentUserCanUploadMedia(siteModel)) {
                    editorMediaActions.launchCamera()
                } else {
                    showNoUploadPermissionSnackbar()
                }
            }
            is MediaPickerAction.OpenSystemPicker -> {
                if (WPMediaUtils.currentUserCanUploadMedia(siteModel)) {
                    allowMultipleSelection = action.allowMultipleSelection
                    WPMediaUtils.launchMediaLibrary(activity, action.allowMultipleSelection)
                } else {
                    showNoUploadPermissionSnackbar()
                }
            }
            is MediaPickerAction.SwitchMediaPicker -> {
                // Handle switching to different data sources (WP Media, Stock, GIF)
                val setup = action.mediaPickerSetup
                when (setup.primaryDataSource) {
                    DataSource.WP_LIBRARY -> {
                        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
                            activity,
                            siteModel,
                            MediaBrowserType.EDITOR_PICKER
                        )
                    }
                    DataSource.STOCK_LIBRARY -> {
                        val requestCode = if (setup.canMultiselect) {
                            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT
                        } else {
                            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK
                        }
                        mediaPickerLauncher.showStockMediaPickerForResult(
                            activity,
                            siteModel,
                            requestCode,
                            setup.canMultiselect
                        )
                    }
                    DataSource.GIF_LIBRARY -> {
                        mediaPickerLauncher.showGifPickerForResult(
                            activity,
                            siteModel,
                            setup.canMultiselect
                        )
                    }
                    else -> { /* Device is handled by OpenSystemPicker */ }
                }
            }
        }
    }

    fun showNoUploadPermissionSnackbar() {
        activity.findViewById<View>(R.id.editor_activity)?.let { view ->
            WPSnackbar.make(
                view = view,
                textRes = R.string.media_error_no_permission_upload,
                duration = Snackbar.LENGTH_SHORT).show()
        }
    }

    // endregion

    companion object {
        private const val PORTRAIT_HEIGHT_RATIO = 0.5f
    }
}
