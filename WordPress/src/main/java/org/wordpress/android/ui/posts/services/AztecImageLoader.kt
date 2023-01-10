@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.widget.ImageView.ScaleType.FIT_CENTER
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.transition.Transition
import org.wordpress.android.util.image.ImageManager
import org.wordpress.aztec.Html
import java.lang.ref.WeakReference

/**
 * Class which retrieves images from both local storage and remote url and informs the client about the progress
 * using Html.ImageGetter.Callbacks.
 *
 * Since Aztec doesn't allow directly setting an image into a View a custom Target which we load
 * the image into needs to be created.
 *
 * It's recommended to explicitly call clearTargets() method, so any resources may be cleared and all pending requests
 * canceled when the screen is not visible to the user anymore. Each target must be stored in a WeakReference,
 * so targets which are not used anymore don't leak.
 */
class AztecImageLoader(
    private val context: Context,
    private val imageManager: ImageManager,
    private val loadingInProgress: Drawable
) : Html.ImageGetter {
    @Suppress("DEPRECATION")
    private val targets = ArrayList<WeakReference<BaseTarget<Bitmap>>>()
    private val mRequestsInProgress = ArrayList<String>()

    override fun loadImage(url: String, callbacks: Html.ImageGetter.Callbacks, maxWidth: Int) {
        loadImage(url, callbacks, maxWidth, 0)
    }

    override fun loadImage(url: String, callbacks: Html.ImageGetter.Callbacks, maxSize: Int, minWidth: Int) {
        mRequestsInProgress.add(url)

        @Suppress("DEPRECATION") val target = object : BaseTarget<Bitmap>() {
            override fun onLoadStarted(placeholder: Drawable?) {
                callbacks.onImageLoading(loadingInProgress)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                callbacks.onImageFailed()
                mRequestsInProgress.remove(url)
            }

            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                val result: Drawable
                // By default, BitmapFactory.decodeFile sets the bitmap's density
                // to the device default so, we need to correctly set the input density to 160 ourselves.
                resource.density = DisplayMetrics.DENSITY_DEFAULT
                val bitmapDrawable = BitmapDrawable(context.resources, resource)
                result = bitmapDrawable
                callbacks.onImageLoaded(result)
                mRequestsInProgress.remove(url)
            }

            override fun getSize(cb: SizeReadyCallback) {
                cb.onSizeReady(maxSize, maxSize)
            }

            override fun removeCallback(cb: SizeReadyCallback) {
                // since we don't store the callback, we don't need to do anything
            }
        }

        imageManager.loadAsBitmapIntoCustomTarget(context, target, url, FIT_CENTER)
        targets.add(WeakReference(target))
    }

    /**
     * Cancel any pending requests and free any resources so they may be reused.
     */
    fun clearTargets() {
        for (weakReference in targets) {
            imageManager.cancelRequest(context, weakReference.get())
        }
    }

    fun getNumberOfImagesBeingDownloaded(): Int {
        return mRequestsInProgress.count()
    }
}
