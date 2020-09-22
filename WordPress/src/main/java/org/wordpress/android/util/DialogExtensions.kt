package org.wordpress.android.util

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.view.View
import org.wordpress.android.R

@SuppressLint("InlinedApi")
fun Dialog.getPreferenceDialogContainerView(): View? {
    val containerViewId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        android.R.id.list_container
    } else {
        android.R.id.list
    }

    var view: View? = findViewById(containerViewId)

    // just in case, try to find a container of our own custom dialog
    if (view == null) {
        view = findViewById(R.id.list_editor_parent)
    }

    return view
}
