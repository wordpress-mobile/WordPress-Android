package org.wordpress.android.util.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.ColorInt
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import java.security.MessageDigest

/**
 * A Glide BitmapTransformation that handles portrait images differently
 * from landscape/square images:
 *
 * - Landscape/square: standard CenterCrop (existing behavior)
 * - Portrait (height > width): scales to fit the target height, centers
 *   horizontally, and fills the remaining space with [backgroundColor]
 */
class PortraitAwareCropTransformation(
    @ColorInt private val backgroundColor: Int
) : BitmapTransformation() {
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        if (toTransform.height <= toTransform.width) {
            return TransformationUtils.centerCrop(
                pool, toTransform, outWidth, outHeight
            )
        }

        // Portrait: fit to height, center horizontally
        val result = pool.get(
            outWidth, outHeight, toTransform.config ?: Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor)

        val scale = outHeight.toFloat() / toTransform.height.toFloat()
        val scaledWidth = toTransform.width * scale
        val left = (outWidth - scaledWidth) / 2f
        val destRect = RectF(left, 0f, left + scaledWidth, outHeight.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(toTransform, null, destRect, paint)

        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is PortraitAwareCropTransformation &&
            backgroundColor == other.backgroundColor
    }

    override fun hashCode(): Int = ID.hashCode() + backgroundColor

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + backgroundColor).toByteArray(CHARSET))
    }

    companion object {
        private const val ID =
            "org.wordpress.android.util.image" +
                ".PortraitAwareCropTransformation"
    }
}
