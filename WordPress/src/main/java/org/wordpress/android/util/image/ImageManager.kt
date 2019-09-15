package org.wordpress.android.util.image

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.ImageView.ScaleType.FIT_XY
import android.widget.ImageView.ScaleType.MATRIX
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.signature.ObjectKey
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
class ImageManager @Inject constructor(private val placeholderManager: ImagePlaceholderManager) {
    interface RequestListener<T> {
        fun onLoadFailed(e: Exception?)
        fun onResourceReady(resource: T)
    }

    /**
     * Return true if this [Context] is available.
     * Availability is defined as the following:
     * + [Context] is not null
     * + [Context] is not destroyed (tested with [FragmentActivity.isDestroyed] or [Activity.isDestroyed])
     */
    private fun Context?.isAvailable(): Boolean {
        if (this == null) {
            return false
        } else if (this !is Application) {
            if (this is FragmentActivity) {
                return !this.isDestroyed
            } else if (this is Activity) {
                return !this.isDestroyed
            }
        }
        return true
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView. Adds a placeholder and an error placeholder depending
     * on the ImageType.
     *
     * If no URL is provided, it only loads the placeholder
     */
    @JvmOverloads
    fun load(imageView: ImageView, imageType: ImageType, imgUrl: String = "", scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the AppWidgetTarget. Adds a placeholder and an error placeholder depending
     * on the ImageType.
     *
     * If no URL is provided, it only loads the placeholder
     */
    @JvmOverloads
    fun load(
        awt: AppWidgetTarget,
        context: Context,
        imageType: ImageType,
        imgUrl: String = "",
        scaleType: ScaleType = CENTER,
        width: Int? = null,
        height: Int? = null
    ) {
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .asBitmap()
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .applyScaleType(scaleType)
                .applySize(width, height)
                .into(awt)
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView and applies circle transformation. Adds placeholder and
     * error placeholder depending on the ImageType.
     */
    @JvmOverloads
    fun loadIntoCircle(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        requestListener: RequestListener<Drawable>? = null,
        version: Int? = null
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .circleCrop()
                .attachRequestListener(requestListener)
                .addSignature(version)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView. Adds a placeholder and an error placeholder depending
     * on the ImageType. Attaches the ResultListener so the client can manually show/hide progress and error
     * views or add a PhotoViewAttacher(adds support for pinch-to-zoom gesture). Optionally adds
     * thumbnailUrl - mostly used for loading low resolution images.
     *
     * Unless you necessarily need to react on the request result, preferred way is to use one of the load(...) methods.
     */
    fun loadWithResultListener(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        scaleType: ScaleType = CENTER,
        thumbnailUrl: String? = null,
        requestListener: RequestListener<Drawable>
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .addThumbnail(context, thumbnailUrl, requestListener)
                .applyScaleType(scaleType)
                .attachRequestListener(requestListener)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads the Bitmap into the ImageView.
     */
    @JvmOverloads
    fun load(imageView: ImageView, bitmap: Bitmap, scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(bitmap)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads the Drawable into the ImageView.
     */
    @JvmOverloads
    fun load(imageView: ImageView, drawable: Drawable, scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(drawable)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads the DrawableResource into the ImageView.
     */
    @JvmOverloads
    fun load(imageView: ImageView, @DrawableRes resourceId: Int, scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(resourceId)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ViewTarget. Adds a placeholder and an error placeholder depending
     * on the ImageType.
     *
     * Use this method with caution and only when you necessarily need it(in other words, don't use it
     * when you need to load an image into an ImageView).
     */
    fun loadIntoCustomTarget(viewTarget: ViewTarget<TextView, Drawable>, imageType: ImageType, imgUrl: String) {
        val context = WordPress.getContext()
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .into(viewTarget)
                .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ViewTarget.
     *
     * Use this method with caution and only when you necessarily need it(in other words, don't use it
     * when you need to load an image into an ImageView).
     */
    fun loadAsBitmapIntoCustomTarget(
        context: Context,
        target: BaseTarget<Bitmap>,
        imgUrl: String,
        scaleType: ScaleType = CENTER
    ) {
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .asBitmap()
                .load(imgUrl)
                .applyScaleType(scaleType)
                .into(target)
    }

    /**
     * Cancel any pending requests and free any resources that may have been
     * loaded for the view.
     */
    fun cancelRequestAndClearImageView(imageView: ImageView) {
        GlideApp.with(imageView.context).clear(imageView)
    }

    /**
     * Cancel any pending requests and free any resources that may have been
     * loaded for the view.
     */
    fun <T : Any> cancelRequest(context: Context, target: BaseTarget<T>?) {
        GlideApp.with(context).clear(target)
    }

    private fun <T : Any> GlideRequest<T>.applyScaleType(
        scaleType: ScaleType
    ): GlideRequest<T> {
        return when (scaleType) {
            ScaleType.CENTER_CROP -> this.centerCrop()
            ScaleType.CENTER_INSIDE -> this.centerInside()
            FIT_CENTER -> this.fitCenter()
            CENTER -> this
            FIT_END,
            FIT_START,
            FIT_XY,
            MATRIX -> {
                AppLog.e(AppLog.T.UTILS, String.format("ScaleType %s is not supported.", scaleType.toString()))
                this
            }
        }
    }

    private fun <T : Any> GlideRequest<T>.applySize(width: Int?, height: Int?): GlideRequest<T> {
        return if (width != null && height != null) {
            this.override(width, height)
        } else {
            this
        }
    }

    private fun <T : Any> GlideRequest<T>.addPlaceholder(imageType: ImageType): GlideRequest<T> {
        val placeholderImageRes = placeholderManager.getPlaceholderResource(imageType)
        return if (placeholderImageRes == null) {
            this
        } else {
            this.placeholder(placeholderImageRes)
        }
    }

    private fun <T : Any> GlideRequest<T>.addFallback(imageType: ImageType): GlideRequest<T> {
        val errorImageRes = placeholderManager.getErrorResource(imageType)
        return if (errorImageRes == null) {
            this
        } else {
            this.error(errorImageRes)
        }
    }

    /**
     * Changing the signature invalidates cache.
     */
    private fun <T : Any> GlideRequest<T>.addSignature(signature: Int?): GlideRequest<T> {
        return if (signature == null) {
            this
        } else {
            this.signature(ObjectKey(signature))
        }
    }

    private fun GlideRequest<Drawable>.addThumbnail(
        context: Context,
        thumbnailUrl: String?,
        listener: RequestListener<Drawable>
    ): GlideRequest<Drawable> {
        return if (TextUtils.isEmpty(thumbnailUrl)) {
            this
        } else {
            val thumbnailRequest = GlideApp
                    .with(context)
                    .load(thumbnailUrl)
                    .attachRequestListener(listener)
            return this.thumbnail(thumbnailRequest)
        }
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
