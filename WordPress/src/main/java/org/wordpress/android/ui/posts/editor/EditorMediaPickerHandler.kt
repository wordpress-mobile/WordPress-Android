@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.editor

import android.app.Activity
import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.EditorCameraHelper
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.util.WPMediaUtils
import javax.inject.Inject

/**
 * Handles media picker launching for the post editor.
 *
 * This class extracts media picker launching logic from EditPostActivity to improve
 * testability and separation of concerns. It coordinates with EditorPhotoPicker for
 * embedded picker and MediaPickerLauncher for full-screen pickers.
 */
@Reusable
class EditorMediaPickerHandler @Inject constructor(
    private val mediaPickerLauncher: MediaPickerLauncher,
    private val editorCameraHelper: EditorCameraHelper
) {
    /**
     * Listener for media picker events that require Activity-level handling.
     */
    interface MediaPickerListener {
        fun getActivity(): Activity
        fun getSiteModel(): SiteModel
        fun getPostId(): Int
        fun getEditorPhotoPicker(): EditorPhotoPicker?
        fun launchCamera()
        fun showNoUploadPermissionSnackbar()
    }

    private var listener: MediaPickerListener? = null

    /**
     * Initializes the handler with the required listener.
     */
    fun start(listener: MediaPickerListener) {
        this.listener = listener
    }

    /**
     * Handles the add media button click, toggling the photo picker.
     */
    @Suppress("ReturnCount")
    fun onAddMediaClicked() {
        val photoPicker = listener?.getEditorPhotoPicker() ?: return
        val site = listener?.getSiteModel() ?: return
        val activity = listener?.getActivity() ?: return

        if (photoPicker.isPhotoPickerShowing()) {
            photoPicker.hidePhotoPicker()
        } else if (WPMediaUtils.currentUserCanUploadMedia(site)) {
            photoPicker.showPhotoPicker(site)
        } else {
            // show the WP media library instead of the photo picker if the user doesn't have upload permission
            mediaPickerLauncher.viewWPMediaLibraryPickerForResult(activity, site, MediaBrowserType.EDITOR_PICKER)
        }
    }

    /**
     * Launches the image picker from the WP media library.
     */
    fun onAddMediaImageClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        listener?.getEditorPhotoPicker()?.setAllowMultipleSelection(allowMultipleSelection)
        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
            activity,
            site,
            MediaBrowserType.GUTENBERG_IMAGE_PICKER
        )
    }

    /**
     * Launches the video picker from the WP media library.
     */
    fun onAddMediaVideoClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        listener?.getEditorPhotoPicker()?.setAllowMultipleSelection(allowMultipleSelection)
        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
            activity,
            site,
            MediaBrowserType.GUTENBERG_VIDEO_PICKER
        )
    }

    /**
     * Launches the media library picker.
     */
    fun onAddLibraryMediaClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        listener?.getEditorPhotoPicker()?.setAllowMultipleSelection(allowMultipleSelection)
        if (allowMultipleSelection) {
            mediaPickerLauncher.viewWPMediaLibraryPickerForResult(activity, site, MediaBrowserType.EDITOR_PICKER)
        } else {
            mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
                activity,
                site,
                MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER
            )
        }
    }

    /**
     * Launches the file picker from the media library.
     */
    fun onAddLibraryFileClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        listener?.getEditorPhotoPicker()?.setAllowMultipleSelection(allowMultipleSelection)
        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
            activity,
            site,
            MediaBrowserType.GUTENBERG_SINGLE_FILE_PICKER
        )
    }

    /**
     * Launches the audio file picker from the media library.
     */
    @Suppress("UnusedParameter")
    fun onAddLibraryAudioFileClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
            activity,
            site,
            MediaBrowserType.GUTENBERG_SINGLE_AUDIO_FILE_PICKER
        )
    }

    /**
     * Launches the photo picker for device photos.
     */
    @Suppress("ReturnCount")
    fun onAddPhotoClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        val postId = listener?.getPostId() ?: return
        val browserType = if (allowMultipleSelection) {
            MediaBrowserType.GUTENBERG_IMAGE_PICKER
        } else {
            MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER
        }
        mediaPickerLauncher.showPhotoPickerForResult(activity, browserType, site, postId)
    }

    /**
     * Captures a photo using the device camera.
     */
    fun onCapturePhotoClicked() {
        val site = listener?.getSiteModel() ?: return
        editorCameraHelper.capturePhotoIfAllowed(
            site,
            onLaunchCamera = { listener?.launchCamera() },
            onNoPermission = { listener?.showNoUploadPermissionSnackbar() }
        )
    }

    /**
     * Launches the video picker for device videos.
     */
    @Suppress("ReturnCount")
    fun onAddVideoClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        val postId = listener?.getPostId() ?: return
        val browserType = if (allowMultipleSelection) {
            MediaBrowserType.GUTENBERG_VIDEO_PICKER
        } else {
            MediaBrowserType.GUTENBERG_SINGLE_VIDEO_PICKER
        }
        mediaPickerLauncher.showPhotoPickerForResult(activity, browserType, site, postId)
    }

    /**
     * Launches the device media picker (photos and videos).
     */
    @Suppress("ReturnCount")
    fun onAddDeviceMediaClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        val postId = listener?.getPostId() ?: return
        val browserType = if (allowMultipleSelection) {
            MediaBrowserType.GUTENBERG_MEDIA_PICKER
        } else {
            MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER
        }
        mediaPickerLauncher.showPhotoPickerForResult(activity, browserType, site, postId)
    }

    /**
     * Launches the stock media picker.
     */
    fun onAddStockMediaClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        val requestCode = if (allowMultipleSelection) {
            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT
        } else {
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK
        }
        mediaPickerLauncher.showStockMediaPickerForResult(activity, site, requestCode, allowMultipleSelection)
    }

    /**
     * Launches the GIF picker.
     */
    fun onAddGifClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        mediaPickerLauncher.showGifPickerForResult(activity, site, allowMultipleSelection)
    }

    /**
     * Launches the file picker.
     */
    fun onAddFileClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        mediaPickerLauncher.showFilePicker(activity, allowMultipleSelection, site)
    }

    /**
     * Launches the audio file picker.
     */
    fun onAddAudioFileClicked(allowMultipleSelection: Boolean) {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        mediaPickerLauncher.showAudioFilePicker(activity, allowMultipleSelection, site)
    }

    /**
     * Captures a video using the device camera.
     */
    fun onCaptureVideoClicked() {
        val activity = listener?.getActivity() ?: return
        val site = listener?.getSiteModel() ?: return
        editorCameraHelper.captureVideoIfAllowed(
            activity,
            site,
            onNoPermission = { listener?.showNoUploadPermissionSnackbar() }
        )
    }
}
