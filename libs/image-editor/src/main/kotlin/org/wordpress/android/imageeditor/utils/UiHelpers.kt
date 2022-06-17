package org.wordpress.android.imageeditor.utils

import android.view.View

object UiHelpers {
    fun updateVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
