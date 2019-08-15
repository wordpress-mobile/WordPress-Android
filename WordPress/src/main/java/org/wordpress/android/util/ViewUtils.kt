package org.wordpress.android.util

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.redirectContextClickToLongPressListener() {
    if (VERSION.SDK_INT >= VERSION_CODES.M) {
        this.setOnContextClickListener { it.performLongClick() }
    }
}
