package org.wordpress.android.util

import android.app.Dialog
import android.view.View
import org.wordpress.android.R

fun Dialog.getPreferenceDialogContainerView(): View? {
    var view: View? = findViewById(android.R.id.list_container)

    // just in case, try to find a container of our own custom dialog
    if (view == null) {
        view = findViewById(R.id.list_editor_parent)
    }

    return view
}
