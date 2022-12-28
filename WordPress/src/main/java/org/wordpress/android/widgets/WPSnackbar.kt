package org.wordpress.android.widgets

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.util.AccessibilityUtils

class WPSnackbar {
    companion object {
        @JvmStatic
        fun make(view: View, @StringRes textRes: Int, duration: Int): Snackbar {
            val text: CharSequence = view.resources.getString(textRes)
            return make(view, text, duration)
        }

        @JvmStatic
        fun make(view: View, text: CharSequence, duration: Int) = Snackbar.make(
            view, // CHECKSTYLE IGNORE
            text, AccessibilityUtils.getSnackbarDuration(view.context, duration)
        )
    }
}
