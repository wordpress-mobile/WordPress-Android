package org.wordpress.android.ui.stories

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.MediaPickerProvider
import com.wordpress.stories.compose.SnackbarProvider
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.photopicker.PhotoPickerFragment

class StoryComposerActivity : ComposeLoopFrameActivity(), SnackbarProvider, MediaPickerProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
    }

    override fun showProvidedSnackbar(message: String, actionLabel: String?, callback: () -> Unit) {
        // no op
        // no provided snackbar here given we're not using snackbars from within the Story Creation experience
        // in WPAndroid
    }

    override fun setupRequestCodes(requestCodes: ExternalMediaPickerRequestCodesAndExtraKeys) {
        requestCodes.PHOTO_PICKER = RequestCodes.PHOTO_PICKER
        requestCodes.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED =
                PhotoPickerActivity.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED
        requestCodes.EXTRA_MEDIA_URIS = PhotoPickerActivity.EXTRA_MEDIA_URIS
    }

    override fun showProvidedMediaPicker() {
        val intent = Intent(this, PhotoPickerActivity::class.java)
        intent.putExtra(PhotoPickerFragment.ARG_BROWSER_TYPE, MediaBrowserType.WP_STORIES_MEDIA_PICKER)

        startActivityForResult(
                intent,
                RequestCodes.PHOTO_PICKER,
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}
