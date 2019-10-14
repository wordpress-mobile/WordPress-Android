package org.wordpress.android.ui.posts.editor

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.Orientation
import org.wordpress.android.R
import org.wordpress.android.editor.AztecEditorFragment
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

// TODO: We shouldn't have a reference to the activity
class EditorPhotoPicker<T>(private val activity: T) where T : AppCompatActivity, T: PhotoPickerListener {
    private var mPhotoPickerContainer: View? = null
    private var mPhotoPickerFragment: PhotoPickerFragment? = null
    private var mPhotoPickerOrientation = Configuration.ORIENTATION_UNDEFINED

    /*
     * loads the photo picker fragment, which is hidden until the user taps the media icon
     */
    private fun initPhotoPicker(site: SiteModel, showAztecEditor: Boolean) {
        mPhotoPickerContainer = activity.findViewById(R.id.photo_fragment_container)

        // size the picker before creating the fragment to avoid having it load media now
        resizePhotoPicker()

        mPhotoPickerFragment = activity.supportFragmentManager.findFragmentByTag(PHOTO_PICKER_TAG) as PhotoPickerFragment
        if (mPhotoPickerFragment == null) {
            val mediaBrowserType = if (showAztecEditor) MediaBrowserType.AZTEC_EDITOR_PICKER else MediaBrowserType.EDITOR_PICKER
            mPhotoPickerFragment = PhotoPickerFragment.newInstance(
                    activity,
                    mediaBrowserType,
                    site
            )
        }
        mPhotoPickerFragment?.let { photoPickerFragment ->
            activity.supportFragmentManager
                    .beginTransaction()
                    .add(R.id.photo_fragment_container, photoPickerFragment, PHOTO_PICKER_TAG)
                    .commit()
        }
    }

    fun isPhotoPickerShowing(): Boolean {
        return mPhotoPickerContainer != null && mPhotoPickerContainer?.visibility == View.VISIBLE
    }

    /*
     * user has requested to show the photo picker
     */
    fun showPhotoPicker(site: SiteModel, showAztecEditor: Boolean, mEditorFragment: Any) {
        val isAlreadyShowing = isPhotoPickerShowing()

        // make sure we initialized the photo picker
        if (mPhotoPickerFragment == null) {
            initPhotoPicker(site, showAztecEditor)
        }

        // hide soft keyboard
        ActivityUtils.hideKeyboard(activity)

        // slide in the photo picker
        if (!isAlreadyShowing) {
            AniUtils.animateBottomBar(mPhotoPickerContainer, true, AniUtils.Duration.MEDIUM)
            mPhotoPickerFragment?.refresh()
            mPhotoPickerFragment?.setPhotoPickerListener(activity)
        }

        // animate in the editor overlay
        showOverlay(true)

        (mEditorFragment as? AztecEditorFragment)?.enableMediaMode(true)
    }

    fun hidePhotoPicker(mEditorFragment: Any) {
        if (isPhotoPickerShowing()) {
            mPhotoPickerFragment?.finishActionMode()
            mPhotoPickerFragment?.setPhotoPickerListener(null)
            AniUtils.animateBottomBar(mPhotoPickerContainer, false)
        }

        hideOverlay()

        (mEditorFragment as? AztecEditorFragment)?.enableMediaMode(false)
    }

    /*
     * resizes the photo picker based on device orientation - full height in landscape, half
     * height in portrait
     */
    private fun resizePhotoPicker() {
        if (mPhotoPickerContainer == null) {
            return
        }

        val updatePickerContainerHeight = { newHeight: Int ->
            mPhotoPickerContainer?.let {
                it.layoutParams.height = newHeight
            }
        }

        if (DisplayUtils.isLandscape(activity)) {
            mPhotoPickerOrientation = Configuration.ORIENTATION_LANDSCAPE
            updatePickerContainerHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            mPhotoPickerOrientation = Configuration.ORIENTATION_PORTRAIT
            val displayHeight = DisplayUtils.getDisplayPixelHeight(activity)
            updatePickerContainerHeight((displayHeight * 0.5f).toInt())
        }

        mPhotoPickerFragment?.reload()
    }

    /*
     * shows/hides the overlay which appears atop the editor, which effectively disables it
     */
    fun showOverlay(animate: Boolean) {
        val overlay = activity.findViewById<View>(R.id.view_overlay)
        if (animate) {
            AniUtils.fadeIn(overlay, AniUtils.Duration.MEDIUM)
        } else {
            overlay.visibility = View.VISIBLE
        }
    }

    fun hideOverlay() {
        val overlay = activity.findViewById<View>(R.id.view_overlay)
        overlay.visibility = View.GONE
    }

    fun onMediaToolbarButtonClicked(action: MediaToolbarAction?) {
        if (action == null || !isPhotoPickerShowing()) {
            return
        }

        mPhotoPickerFragment?.let { photoPickerFragment ->
            when (action) {
                MediaToolbarAction.CAMERA -> photoPickerFragment.showCameraPopupMenu(
                        activity.findViewById(action.buttonId)
                )
                MediaToolbarAction.GALLERY -> photoPickerFragment.showPickerPopupMenu(
                        activity.findViewById(action.buttonId)
                )
                MediaToolbarAction.LIBRARY -> photoPickerFragment.doIconClicked(PhotoPickerIcon.WP_MEDIA)
            }
        }
    }

    fun onOrientationChanged(@Orientation newOrientation: Int) {
        // resize the photo picker if the user rotated the device
        if (newOrientation != mPhotoPickerOrientation) {
            resizePhotoPicker()
        }
    }
}
