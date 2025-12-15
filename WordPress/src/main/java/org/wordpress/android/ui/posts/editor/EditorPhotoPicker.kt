package org.wordpress.android.ui.posts.editor

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.Orientation
import org.wordpress.android.R
import org.wordpress.android.editor.MediaToolbarAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaPickerFragment
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.DisplayUtils

private const val MEDIA_PICKER_TAG = "media_picker"

interface EditorPhotoPickerListener {
    fun onPhotoPickerShown()
    fun onPhotoPickerHidden()
}

/**
 * This class is extracted from EditPostActivity as part of a huge refactor. Although some effort was made to improve
 * the code, it still contains the full logic from EditPostActivity to make the refactor less error-prone. As such,
 * it is heavily coupled with the `EditPostActivity` and contains logic and dependencies it shouldn't and in dire need
 * of further refactoring.
 */
class EditorPhotoPicker(
    private val activity: AppCompatActivity,
    private val mediaPickerListener: MediaPickerFragment.MediaPickerListener,
    private val editorPhotoPickerListener: EditorPhotoPickerListener,
    private val showAztecEditor: Boolean
) : MediaToolbarAction.MediaToolbarButtonClickListener {
    private var photoPickerContainer: View? = null

    private var mediaPickerFragment: MediaPickerFragment? = null
    private var photoPickerOrientation = Configuration.ORIENTATION_UNDEFINED
    var allowMultipleSelection: Boolean = false

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
                mediaPickerListener,
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
        return photoPickerContainer != null && photoPickerContainer?.visibility == View.VISIBLE
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
            mediaPickerFragment?.setMediaPickerListener(mediaPickerListener)
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
            updatePickerContainerHeight((displayHeight * 0.5f).toInt())
        }
    }

    override fun onMediaToolbarButtonClicked(action: MediaToolbarAction?) {
        // MediaPickerFragment handles its own toolbar actions through FAB and menu,
        // so this method is no longer needed for the embedded picker.
        // The actions are now handled through MediaPickerListener.onIconClicked()
    }

    fun onOrientationChanged(@Orientation newOrientation: Int) {
        // resize the photo picker if the user rotated the device
        if (newOrientation != photoPickerOrientation) {
            resizePhotoPicker()
        }
    }
}
