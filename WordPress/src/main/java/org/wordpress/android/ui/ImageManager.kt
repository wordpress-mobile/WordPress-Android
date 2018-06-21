package org.wordpress.android.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.CENTER
import org.wordpress.android.modules.GlideApp
import org.wordpress.android.modules.GlideRequest
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton for asynchronous image fetching/loading with support for placeholders, transformations and more.
 */
@Singleton
class ImageManager @Inject constructor() {
    @JvmOverloads
    fun load(
        imageView: ImageView,
        imgUrl: String,
        @DrawableRes placeholder: Int? = null,
        scaleType: ImageView.ScaleType = CENTER
    ) {
        var request = GlideApp.with(imageView.context)
                .load(imgUrl)
                .let { if (placeholder != null) it.fallback(placeholder) else it }
        request = applyScaleType(request, scaleType)
        request.into(imageView)
    }

    @JvmOverloads
    fun load(imageView: ImageView, bitmap: Bitmap, scaleType: ImageView.ScaleType = CENTER) {
        var request = GlideApp.with(imageView.context)
                .load(bitmap)
        request = applyScaleType(request, scaleType)
        request.into(imageView)
    }

    @JvmOverloads
    fun load(imageView: ImageView, imgUrl: Drawable, scaleType: ImageView.ScaleType = CENTER) {
        var request = GlideApp.with(imageView.context)
                .load(imgUrl)
        request = applyScaleType(request, scaleType)
        request.into(imageView)
    }

    @JvmOverloads
    fun loadIntoCircle(imageView: ImageView, imgUrl: String, @DrawableRes placeholder: Int? = null) {
        val request = GlideApp.with(imageView.context)
                .load(imgUrl)
                .let { if (placeholder != null) it.fallback(placeholder) else it }
                .circleCrop()
        request.into(imageView)
    }

    fun cancelRequestAndClearImageView(imageView: ImageView) {
        GlideApp.with(imageView.context).clear(imageView)
    }

    private fun applyScaleType(
        request: GlideRequest<Drawable>,
        scaleType: ScaleType
    ): GlideRequest<Drawable> {
        return when (scaleType) {
            ImageView.ScaleType.CENTER_CROP -> request.centerCrop()
            ImageView.ScaleType.CENTER_INSIDE -> request.centerInside()
            ImageView.ScaleType.FIT_CENTER -> request.fitCenter()
            ImageView.ScaleType.CENTER -> request
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.MATRIX -> {
                AppLog.e(AppLog.T.UTILS, String.format("ScaleType %s is not supported.", scaleType.toString()))
                request
            }
        }
    }

    @Deprecated("Object for backward compatibility with code which doesn't support DI")
    companion object {
        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.load(imageView, imgUrl, placeholder, scaleType)",
                        "org.wordpress.android.ui.ImageManager"))
        @JvmOverloads
        fun loadImage(
            imageView: ImageView,
            imgUrl: String,
            @DrawableRes placeholder: Int? = null,
            scaleType: ImageView.ScaleType
        ) {
            ImageManager().load(imageView, imgUrl, placeholder, scaleType)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.load(imageView, bitmap, placeholder, scaleType)",
                        "org.wordpress.android.ui.ImageManager"))
        @JvmOverloads
        fun loadImage(imageView: ImageView, bitmap: Bitmap, scaleType: ImageView.ScaleType = CENTER) {
            ImageManager().load(imageView, bitmap, scaleType)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.load(imageView, drawable, placeholder, scaleType)",
                        "org.wordpress.android.ui.ImageManager"))
        @JvmOverloads
        fun loadImage(imageView: ImageView, drawable: Drawable, scaleType: ImageView.ScaleType = CENTER) {
            ImageManager().load(imageView, drawable, scaleType)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.loadIntoCircle(imageView, imgUrl, placeholder)",
                        "org.wordpress.android.ui.ImageManager"))
        @JvmOverloads
        fun loadImageIntoCircle(imageView: ImageView, imgUrl: String, @DrawableRes placeholder: Int? = null) {
            ImageManager().loadIntoCircle(imageView, imgUrl, placeholder)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.clear(imageView)",
                        "org.wordpress.android.ui.ImageManager"))
        fun clear(imageView: ImageView) {
            ImageManager().cancelRequestAndClearImageView(imageView)
        }
    }
}
