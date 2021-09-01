package org.wordpress.android.util

import android.view.View
import android.view.View.OnClickListener
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.ui.utils.UiString
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

// Taken from com.google.android.material.snackbar.SnackbarManager.java
// Did not find a way to get them directly from the android framework for now
private const val SHORT_DURATION_MS = 1500L
private const val LONG_DURATION_MS = 2750L

const val INDEFINITE_SNACKBAR_NOT_ALLOWED = "Snackbar.LENGTH_INDEFINITE not allowed in getSnackbarDurationMs."

class SnackbarItem(
    val info: Info,
    val action: Action? = null,
    dismissCallback: ((transientBottomBar: Snackbar?, event: Int) -> Unit)? = null,
    showCallback: ((transientBottomBar: Snackbar?) -> Unit)? = null
) {
    val dismissCallback = SoftReference(dismissCallback)
    val showCallback = SoftReference(showCallback)

    fun getSnackbarDurationMs(): Long {
        return when (info.duration) {
            Snackbar.LENGTH_INDEFINITE ->
                throw IllegalArgumentException(INDEFINITE_SNACKBAR_NOT_ALLOWED)
            Snackbar.LENGTH_LONG -> LONG_DURATION_MS
            Snackbar.LENGTH_SHORT -> SHORT_DURATION_MS
            else -> info.duration.toLong()
        }
    }

    class Info(
        view: View,
        val textRes: UiString,
        val duration: Int,
        val isImportant: Boolean = true
    ) {
        val view = WeakReference(view)
    }

    class Action(
        val textRes: UiString,
        clickListener: OnClickListener
    ) {
        val clickListener = SoftReference(clickListener)
    }

    val snackbarCallback = object : Snackbar.Callback() {
        override fun onShown(transientBottomBar: Snackbar?) {
            this@SnackbarItem.showCallback.get()?.invoke(transientBottomBar)
            super.onShown(transientBottomBar)
        }

        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            this@SnackbarItem.dismissCallback.get()?.invoke(transientBottomBar, event)
            super.onDismissed(transientBottomBar, event)
        }
    }
}
