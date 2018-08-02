package org.wordpress.android.util.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.CENTER
import android.widget.TextView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import org.wordpress.android.WordPress
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
    interface RequestListener<T> {
        fun onLoadFailed(e: Exception?)
        fun onResourceReady(resource: T)
    }

    @JvmOverloads
    fun load(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        scaleType: ImageView.ScaleType = CENTER,
        requestListener: RequestListener<Drawable>? = null
    ) {
        GlideApp.with(imageView.context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .applyScaleType(scaleType)
                .attachRequestListener(requestListener)
                .into(imageView)
                .clearOnDetach()
    }

    @JvmOverloads
    fun load(imageView: ImageView, bitmap: Bitmap, scaleType: ImageView.ScaleType = CENTER) {
        GlideApp.with(imageView.context)
                .load(bitmap)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    @JvmOverloads
    fun load(imageView: ImageView, drawable: Drawable, scaleType: ImageView.ScaleType = CENTER) {
        GlideApp.with(imageView.context)
                .load(drawable)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    @JvmOverloads
    fun load(imageView: ImageView, resourceId: Int, scaleType: ImageView.ScaleType = CENTER) {
        GlideApp.with(imageView.context)
                .load(resourceId)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    fun load(viewTarget: ViewTarget<TextView, Drawable>, imageType: ImageType, imgUrl: String) {
        GlideApp.with(WordPress.getContext())
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .into(viewTarget)
                .clearOnDetach()
    }

    fun loadAsBitmapIntoCustomTarget(
        context: Context,
        target: BaseTarget<Bitmap>,
        imgUrl: String,
        scaleType: ImageView.ScaleType = CENTER
    ) {
        GlideApp.with(context)
                .asBitmap()
                .load(imgUrl)
                .applyScaleType(scaleType)
                .into(target)
    }

    fun loadIntoCircle(imageView: ImageView, imageType: ImageType, imgUrl: String) {
        GlideApp.with(imageView.context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .circleCrop()
                .into(imageView)
                .clearOnDetach()
    }

    fun cancelRequestAndClearImageView(imageView: ImageView) {
        GlideApp.with(imageView.context).clear(imageView)
    }

    fun <T : Any> cancelRequest(context: Context, target: BaseTarget<T>?) {
        GlideApp.with(context).clear(target)
    }

    private fun <T : Any> GlideRequest<T>.applyScaleType(
        scaleType: ScaleType
    ): GlideRequest<T> {
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

    private fun <T : Any> GlideRequest<T>.addPlaceholder(imageType: ImageType): GlideRequest<T> {
        val placeholderImageRes = placeholderManager.getPlaceholderResource(imageType)
        return if (placeholderImageRes == null) this else this.placeholder(placeholderImageRes)
    }

    private fun <T : Any> GlideRequest<T>.addFallback(imageType: ImageType): GlideRequest<T> {
        val errorImageRes = placeholderManager.getErrorResource(imageType)
        return if (errorImageRes == null) this else this.error(errorImageRes)
    }

    private fun <T : Any> GlideRequest<T>.attachRequestListener(
        requestListener: RequestListener<T>?
    ): GlideRequest<T> {
        return if (requestListener == null) {
            this
        } else {
            this.listener(object : com.bumptech.glide.request.RequestListener<T> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<T>?,
                    isFirstResource: Boolean
                ): Boolean {
                    requestListener.onLoadFailed(e)
                    return false
                }

                override fun onResourceReady(
                    resource: T?,
                    model: Any?,
                    target: Target<T>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (resource != null) {
                        requestListener.onResourceReady(resource)
                    } else {
                        // according to the Glide's JavaDoc, this shouldn't happen
                        AppLog.e(AppLog.T.UTILS, "Resource in ImageManager.onResourceReady is null.")
                        requestListener.onLoadFailed(null)
                    }
                    return false
                }
            })
        }
    }

    @Deprecated("Object for backward compatibility with code which doesn't support DI")
    companion object {
        @JvmStatic
        @Deprecated("Use injected ImageManager")
        val instance: ImageManager by lazy { ImageManager(ImagePlaceholderManager()) }
    }
}
