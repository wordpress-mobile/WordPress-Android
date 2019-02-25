package org.wordpress.android.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

object ColorUtils {
    fun applyTintToDrawable(context: Context, @DrawableRes drawableResId: Int, @ColorRes colorResId: Int): Drawable {
        val drawable = context.resources.getDrawable(drawableResId, context.theme)
        val color = ContextCompat.getColor(context, colorResId)
        DrawableCompat.setTint(drawable, color)
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN)
        return drawable
    }

    fun setImageResourceWithTint(imageView: ImageView, @DrawableRes drawableResId: Int, @ColorRes colorResId: Int) {
        imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(imageView.context, colorResId))
        imageView.setImageResource(drawableResId)
    }
}
