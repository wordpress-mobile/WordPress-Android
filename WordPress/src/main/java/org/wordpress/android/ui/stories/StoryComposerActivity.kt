package org.wordpress.android.ui.stories

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.MediaPickerProvider
import com.wordpress.stories.compose.SnackbarProvider
import org.wordpress.android.widgets.WPSnackbar

class StoryComposerActivity : ComposeLoopFrameActivity(), SnackbarProvider, MediaPickerProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
    }

    override fun showProvidedSnackbar(message: String, actionLabel: String?, callback: () -> Unit) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = WPSnackbar.make(view, message, Snackbar.LENGTH_LONG)
        snackbar.setAction(actionLabel) { callback }
        snackbar.show()
    }

    override fun setupRequestCodes(requestCodes: ExternalMediaPickerRequestCodesAndExtraKeys) {
        // TODO implement request codes
    }

    override fun showProvidedMediaPicker() {
        // TODO implement show media picker
        Toast.makeText(this, "picker not implemeneted yet", Toast.LENGTH_SHORT).show()
    }
}
