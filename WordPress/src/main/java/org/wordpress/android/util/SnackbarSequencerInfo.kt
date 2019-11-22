package org.wordpress.android.util

import android.view.View
import android.view.View.OnClickListener
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.ui.utils.UiString
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

class SnackbarActionInfo(
    val textRes: UiString,
    clickListener: OnClickListener
) {
    val clickListener = SoftReference(clickListener)
}

class SnackbarCallbackInfo(
    snackbarCallback: Snackbar.Callback
) {
    val snackbarCallback = SoftReference(snackbarCallback)
}

class SnackbarInfo(
    view: View,
    val textRes: UiString,
    val duration: Int
) {
    val view = WeakReference(view)
}

class SnackbarSequencerInfo(
    val snackbarInfo: SnackbarInfo,
    val snackbarActionInfo: SnackbarActionInfo? = null,
    val snackbarCallbackInfo: SnackbarCallbackInfo? = null
)
