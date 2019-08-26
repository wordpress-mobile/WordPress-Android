package org.wordpress.android.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

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

@ColorRes
fun Context.getThemeColorResId(@AttrRes attribute: Int) =
        TypedValue().let {
            theme.resolveAttribute(attribute, it, true)
            it.resourceId
        }

@ColorInt
fun Context.getThemeColor(@AttrRes attribute: Int) =
        TypedValue().let {
            theme.resolveAttribute(attribute, it, true)
            it.data
        }
