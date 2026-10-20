package org.wordpress.android.util.extensions

import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.redirectContextClickToLongPressListener() {
    this.setOnContextClickListener { it.performLongClick() }
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

fun RecyclerView.disableAnimation() {
    (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
}

fun View.getString(@StringRes stringRes: Int) = context.getString(stringRes)
