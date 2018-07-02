package org.wordpress.android.util.image

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
class ImageManager @Inject constructor(val placeholderManager: ImagePlaceholderManager) {
    @JvmOverloads
    fun load(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        scaleType: ImageView.ScaleType = CENTER
    ) {
        var request = GlideApp.with(imageView.context)
                .load(imgUrl)
        request = addFallback(request, imageType)
        request = addPlaceholder(request, imageType)
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
    fun load(imageView: ImageView, drawable: Drawable, scaleType: ImageView.ScaleType = CENTER) {
        var request = GlideApp.with(imageView.context)
                .load(drawable)
        request = applyScaleType(request, scaleType)
        request.into(imageView)
    }

    fun loadIntoCircle(imageView: ImageView, imageType: ImageType, imgUrl: String) {
        var request = GlideApp.with(imageView.context)
                .load(imgUrl)
        request = addFallback(request, imageType)
        request = addPlaceholder(request, imageType)
        request.circleCrop().into(imageView)
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

    private fun addPlaceholder(request: GlideRequest<Drawable>, imageType: ImageType): GlideRequest<Drawable> {
        val placeholderImageRes = placeholderManager.getPlaceholderImage(imageType)
        return if (placeholderImageRes == null) request else request.placeholder(placeholderImageRes)
    }

    private fun addFallback(request: GlideRequest<Drawable>, imageType: ImageType): GlideRequest<Drawable> {
        val errorImageRes = placeholderManager.getErrorImage(imageType)
        return if (errorImageRes == null) request else request.error(errorImageRes)
    }

    @Deprecated("Object for backward compatibility with code which doesn't support DI")
    companion object {
        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.load(imageView, imgUrl, placeholder, scaleType)",
                        "org.wordpress.android.util.image.ImageManager"))
        fun loadImage(
            imageView: ImageView,
            imageType: ImageType,
            imgUrl: String,
            scaleType: ImageView.ScaleType
        ) {
            ImageManager(ImagePlaceholderManager()).load(imageView, imageType, imgUrl, scaleType)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.load(imageView, bitmap, scaleType)",
                        "org.wordpress.android.util.image.ImageManager"))
        @JvmOverloads
        fun loadImage(imageView: ImageView, bitmap: Bitmap, scaleType: ImageView.ScaleType = CENTER) {
            ImageManager(ImagePlaceholderManager()).load(imageView, bitmap, scaleType)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.load(imageView, drawable, scaleType)",
                        "org.wordpress.android.util.image.ImageManager"))
        @JvmOverloads
        fun loadImage(imageView: ImageView, drawable: Drawable, scaleType: ImageView.ScaleType = CENTER) {
            ImageManager(ImagePlaceholderManager()).load(imageView, drawable, scaleType)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.loadIntoCircle(imageView, imgType, imgUrl)",
                        "org.wordpress.android.util.image.ImageManager"))
        fun loadImageIntoCircle(imageView: ImageView, imageType: ImageType, imgUrl: String) {
            ImageManager(ImagePlaceholderManager()).loadIntoCircle(imageView, imageType, imgUrl)
        }

        @JvmStatic
        @Deprecated("Use injected ImageManager",
                ReplaceWith("imageManager.clear(imageView)",
                        "org.wordpress.android.util.image.ImageManager"))
        fun clear(imageView: ImageView) {
            ImageManager(ImagePlaceholderManager()).cancelRequestAndClearImageView(imageView)
        }
    }
}
