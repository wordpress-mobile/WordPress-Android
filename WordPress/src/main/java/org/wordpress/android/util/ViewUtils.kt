package org.wordpress.android.util

import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.TouchDelegate
import android.view.View
import androidx.annotation.DimenRes

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.redirectContextClickToLongPressListener() {
    if (VERSION.SDK_INT >= VERSION_CODES.M) {
        this.setOnContextClickListener { it.performLongClick() }
    }
}

fun View.expandTouchTargetArea(@DimenRes dimenRes: Int, heightOnly: Boolean = false) {
    val pixels = context.resources.getDimensionPixelSize(dimenRes)
    val parent = this.parent as View

    parent.post {
        val touchTargetRect = Rect()
        getHitRect(touchTargetRect)
        touchTargetRect.top -= pixels
        touchTargetRect.bottom += pixels

        if (!heightOnly) {
            touchTargetRect.right += pixels
            touchTargetRect.left -= pixels
        }

        parent.touchDelegate = TouchDelegate(touchTargetRect, this)
    }
}
