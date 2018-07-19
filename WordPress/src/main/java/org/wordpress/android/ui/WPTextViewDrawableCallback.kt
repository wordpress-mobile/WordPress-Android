package org.wordpress.android.ui

import android.graphics.drawable.Drawable
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * Class extends Drawable.Callback, so we can support animated drawables (gifs).
 * Upon retrieving a drawable, call Drawable.setCallback(...).
 */
class WPTextViewDrawableCallback(textView: TextView) : Drawable.Callback {
    private val weakView: WeakReference<TextView> = WeakReference(textView)

    override fun invalidateDrawable(who: Drawable) {
        weakView.get()?.invalidate()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {}
}
