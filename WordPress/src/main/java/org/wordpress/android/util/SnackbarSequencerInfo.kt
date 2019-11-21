package org.wordpress.android.util

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.ui.utils.UiString
import java.lang.ref.WeakReference

class SnackbarActionInfo(
    val textRes: UiString,
    //actionListener: (() -> Unit)
    //clickListener: ((View) -> Unit)? = null
    val clickListener: WeakReference<OnClickListener>
) {
    //val actionListener = WeakReference(actionListener)
    //val clickListener = WeakReference(clickListener)
}

class SnacbarCallbackInfo(
    val snackbarCallback: WeakReference<Snackbar.Callback>
) {
    //val snackbarCallback = WeakReference(snackbarCallback)
}

data class SnackbarInfo(
    val view: WeakReference<View>,
    val textRes: UiString,
    val duration: Int
)


data class SnackbarSequencerInfo(
    val context: WeakReference<Context>,
    val snackbarInfo: SnackbarInfo,
    val snackbarActionInfo: SnackbarActionInfo? = null,
    val snackbarCallbackInfo: SnacbarCallbackInfo? = null,
    val creationTimestamp: Long
)
