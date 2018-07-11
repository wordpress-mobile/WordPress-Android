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
        GlideApp.with(imageView.context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .applyScaleType(scaleType)
                .into(imageView)
    }

    @JvmOverloads
    fun load(imageView: ImageView, bitmap: Bitmap, scaleType: ImageView.ScaleType = CENTER) {
        GlideApp.with(imageView.context)
                .load(bitmap)
                .applyScaleType(scaleType)
                .into(imageView)
    }

    @JvmOverloads
    fun load(imageView: ImageView, drawable: Drawable, scaleType: ImageView.ScaleType = CENTER) {
        GlideApp.with(imageView.context)
                .load(drawable)
                .applyScaleType(scaleType)
                .into(imageView)
    }

    @JvmOverloads
    fun load(imageView: ImageView, resourceId: Int, scaleType: ImageView.ScaleType = CENTER) {
        GlideApp.with(imageView.context)
                .load(resourceId)
                .applyScaleType(scaleType)
                .into(imageView)
    }

    fun loadIntoCircle(imageView: ImageView, imageType: ImageType, imgUrl: String) {
        GlideApp.with(imageView.context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .circleCrop()
                .into(imageView)
    }

    fun cancelRequestAndClearImageView(imageView: ImageView) {
        GlideApp.with(imageView.context).clear(imageView)
    }

    private fun GlideRequest<Drawable>.applyScaleType(
        scaleType: ScaleType
    ): GlideRequest<Drawable> {
        return when (scaleType) {
            ImageView.ScaleType.CENTER_CROP -> this.centerCrop()
            ImageView.ScaleType.CENTER_INSIDE -> this.centerInside()
            ImageView.ScaleType.FIT_CENTER -> this.fitCenter()
            ImageView.ScaleType.CENTER -> this
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.MATRIX -> {
                AppLog.e(AppLog.T.UTILS, String.format("ScaleType %s is not supported.", scaleType.toString()))
                this
            }
        }
    }

    private fun GlideRequest<Drawable>.addPlaceholder(imageType: ImageType): GlideRequest<Drawable> {
        val placeholderImageRes = placeholderManager.getPlaceholderResource(imageType)
        return if (placeholderImageRes == null) this else this.placeholder(placeholderImageRes)
    }

    private fun GlideRequest<Drawable>.addFallback(imageType: ImageType): GlideRequest<Drawable> {
        val errorImageRes = placeholderManager.getErrorResource(imageType)
        return if (errorImageRes == null) this else this.error(errorImageRes)
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
    }
}
