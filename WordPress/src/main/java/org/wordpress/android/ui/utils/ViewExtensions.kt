package org.wordpress.android.ui.utils

import android.view.View

fun View.show(isVisible: Boolean) {
    visibility = if (isVisible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}
