package org.wordpress.android.util.extensions

import android.app.Dialog
import android.view.View
import org.wordpress.android.R
import org.wordpress.android.R.attr

fun Dialog.getPreferenceDialogContainerView(): View? {
    var view: View? = findViewById(android.R.id.list_container)

    // just in case, try to find a container of our own custom dialog
    if (view == null) {
        view = findViewById(R.id.list_editor_parent)
    }

    return view
}

@Suppress("DEPRECATION")
fun Dialog.setStatusBarAsSurfaceColor() {
    window?.apply {
        statusBarColor = context.getColorFromAttribute(attr.colorSurface)
        if (!context.resources.configuration.isDarkTheme()) {
            decorView.systemUiVisibility = decorView
                .systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}
