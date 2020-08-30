package org.wordpress.android.ui.posts.editor

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.Orientation
import org.wordpress.android.R
import org.wordpress.android.editor.MediaToolbarAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.PhotoPickerFragment
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerListener
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.DisplayUtils

private const val PHOTO_PICKER_TAG = "photo_picker"

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
    private val photoPickerListener: PhotoPickerListener,
    private val editorPhotoPickerListener: EditorPhotoPickerListener,
    private val showAztecEditor: Boolean
) : MediaToolbarAction.MediaToolbarButtonClickListener {
    private var photoPickerContainer: View? = null
    private var photoPickerFragment: PhotoPickerFragment? = null
    private var photoPickerOrientation = Configuration.ORIENTATION_UNDEFINED
    var allowMultipleSelection: Boolean = false

    /*
     * loads the photo picker fragment, which is hidden until the user taps the media icon
     */
    private fun initPhotoPicker(site: SiteModel) {
        photoPickerContainer = activity.findViewById(R.id.photo_fragment_container)

        // size the picker before creating the fragment to avoid having it load media now
        resizePhotoPicker()

        photoPickerFragment = activity.supportFragmentManager.findFragmentByTag(PHOTO_PICKER_TAG)
                as? PhotoPickerFragment
        if (photoPickerFragment == null) {
            val mediaBrowserType = if (showAztecEditor) {
                MediaBrowserType.AZTEC_EDITOR_PICKER
            } else MediaBrowserType.EDITOR_PICKER

            PhotoPickerFragment.newInstance(
                    photoPickerListener,
                    mediaBrowserType,
                    site
            ).let {
                photoPickerFragment = it
                activity.supportFragmentManager
                        .beginTransaction()
                        .add(R.id.photo_fragment_container, it, PHOTO_PICKER_TAG)
                        .commit()
            }
        }
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
        if (photoPickerFragment == null) {
            initPhotoPicker(site)
        }

        // hide soft keyboard
        ActivityUtils.hideKeyboard(activity)

        // slide in the photo picker
        if (!isAlreadyShowing) {
            AniUtils.animateBottomBar(photoPickerContainer, true, AniUtils.Duration.MEDIUM)
            photoPickerFragment?.refresh()
            photoPickerFragment?.setPhotoPickerListener(photoPickerListener)
        }

        editorPhotoPickerListener.onPhotoPickerShown()
    }

    fun hidePhotoPicker() {
        if (isPhotoPickerShowing()) {
            photoPickerFragment?.finishActionMode()
            photoPickerFragment?.setPhotoPickerListener(null)
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
            val displayHeight = DisplayUtils.getDisplayPixelHeight(activity)
            updatePickerContainerHeight((displayHeight * 0.5f).toInt())
        }
    }

    override fun onMediaToolbarButtonClicked(action: MediaToolbarAction?) {
        if (action == null || !isPhotoPickerShowing()) {
            return
        }

        photoPickerFragment?.let { photoPickerFragment ->
            when (action) {
                MediaToolbarAction.CAMERA -> photoPickerFragment.showCameraPopupMenu(
                        activity.findViewById(action.buttonId)
                )
                MediaToolbarAction.GALLERY -> photoPickerFragment.performActionOrShowPopup(
                        activity.findViewById(action.buttonId)
                )
                MediaToolbarAction.LIBRARY -> photoPickerFragment.doIconClicked(PhotoPickerIcon.WP_MEDIA)
            }
        }
    }

    fun onOrientationChanged(@Orientation newOrientation: Int) {
        // resize the photo picker if the user rotated the device
        if (newOrientation != photoPickerOrientation) {
            resizePhotoPicker()
        }
    }
}
