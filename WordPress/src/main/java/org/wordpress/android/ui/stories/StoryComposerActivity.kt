package org.wordpress.android.ui.stories

import android.os.Bundle
import android.widget.Toast
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.MediaPickerProvider
import com.wordpress.stories.compose.SnackbarProvider

class StoryComposerActivity : ComposeLoopFrameActivity(), SnackbarProvider, MediaPickerProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
    }

    override fun showProvidedSnackbar(message: String, actionLabel: String?, callback: () -> Unit) {
        // TODO implement snackbar
    }

    override fun setupRequestCodes(requestCodes: ExternalMediaPickerRequestCodesAndExtraKeys) {
        // TODO implement request codes
    }

    override fun showProvidedMediaPicker() {
        // TODO implement show media picker
        Toast.makeText(this, "picker not implemeneted yet", Toast.LENGTH_SHORT)
    }
}
