package org.wordpress.android.util

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.view.View

@SuppressLint("InlinedApi")
fun Dialog.getPreferenceDialogContainerView(): View? {
    val containerViewId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        android.R.id.list_container
    } else {
        android.R.id.list
    }

    return findViewById(containerViewId)
}
