package org.wordpress.android.util

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat
import kotlin.math.roundToInt

object ColorUtils {
    @JvmStatic
    fun applyTintToDrawable(context: Context, @DrawableRes drawableResId: Int, @ColorRes colorResId: Int): Drawable {
        val drawable = context.resources.getDrawable(drawableResId, context.theme)
        val color = ContextCompat.getColor(context, colorResId)
        DrawableCompat.setTint(drawable, color)
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN)
        return drawable
    }

    @JvmStatic
    fun setImageResourceWithTint(imageView: ImageView, @DrawableRes drawableResId: Int, @ColorRes colorResId: Int) {
        imageView.setImageDrawable(ContextCompat.getDrawable(imageView.context, drawableResId))
        ImageViewCompat.setImageTintList(
            imageView,
            AppCompatResources.getColorStateList(imageView.context, colorResId)
        )
    }

    @JvmStatic
    @ColorInt
    fun applyEmphasisToColor(
        @ColorInt color: Int,
        @FloatRange(from = 0.0, to = 1.0) emphasisAlpha: Float
    ) = ColorUtils.setAlphaComponent(color, (emphasisAlpha * 255).roundToInt())
}
