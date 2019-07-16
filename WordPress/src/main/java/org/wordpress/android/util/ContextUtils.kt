package org.wordpress.android.util

import android.content.Context
import android.graphics.drawable.Drawable

fun Context.getDrawableFromAttribute(attributeId: Int): Drawable? {
    val styledAttributes = this.obtainStyledAttributes(intArrayOf(attributeId))
    val styledDrawable = styledAttributes.getDrawable(0)
    styledAttributes.recycle()
    return styledDrawable
}
