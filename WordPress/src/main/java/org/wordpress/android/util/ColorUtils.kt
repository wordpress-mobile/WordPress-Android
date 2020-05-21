package org.wordpress.android.util

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat

object ColorUtils {
    fun applyTintToDrawable(context: Context, @DrawableRes drawableResId: Int, @ColorRes colorResId: Int): Drawable {
        val drawable = context.resources.getDrawable(drawableResId, context.theme)
        val color = ContextCompat.getColor(context, colorResId)
        DrawableCompat.setTint(drawable, color)
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN)
        return drawable
    }

    fun setImageResourceWithTint(imageView: ImageView, @DrawableRes drawableResId: Int, @ColorRes colorResId: Int) {
        imageView.setImageDrawable(ContextCompat.getDrawable(imageView.context, drawableResId))
        ImageViewCompat.setImageTintList(
                imageView,
                AppCompatResources.getColorStateList(imageView.context, colorResId)
        )
    }
}
