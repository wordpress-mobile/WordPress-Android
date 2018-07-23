package org.wordpress.android.util.image.getters

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.TextView
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition
import org.wordpress.android.ui.WPTextViewDrawableCallback
import org.wordpress.android.util.ImageUtils

/**
 * A class that we can load a remote resource into. Automatically displays placeholder while the remote img is
 * loading and displays an error image if the loading fails.
 *
 * We could probably subclass BaseTarget instead of ViewTarget, since we basically override most of its functionality.
 * However, we might want to use ViewTarget.clearOnDetach(..) when it becomes stable (it's experimental now).
 * It clears the View's Request when the View is detached from its Window and restarts the Request when the View is
 * re-attached from its Window.
 */
internal class WPRemoteResourceViewTarget(
    view: TextView,
    private val maxSize: Int
) : ViewTarget<TextView, Drawable>(view) {
    private val drawableWrapper = RemoteDrawableWrapper()
    private var request: Request? = null

    val drawable: Drawable get() = drawableWrapper

    override fun onResourceReady(res: Drawable, transition: Transition<in Drawable>?) {
        if (res is Animatable) {
            // Bind a Callback object to this Drawable.  Required for clients that want to support
            // animated drawables.
            res.callback = WPTextViewDrawableCallback(getView())
            (res as Animatable).start()
        }
        replaceDrawable(res, ImageUtils.getScaledBounds(res.intrinsicWidth, res.intrinsicHeight, maxSize))
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        errorDrawable?.let {
            replaceDrawable(it, Rect(0, 0, it.intrinsicWidth, it.intrinsicHeight))
        }
    }

    override fun onLoadStarted(res: Drawable?) {
        super.onLoadStarted(res)
        res?.let {
            replaceDrawable(it, Rect(0, 0, it.intrinsicWidth, it.intrinsicHeight))
        }
    }

    private fun replaceDrawable(drawable: Drawable, bounds: Rect) {
        drawableWrapper.setDrawable(drawable)
        drawableWrapper.bounds = bounds
        // force textView to resize correctly by resetting the content to itself
        getView().text = getView().text
    }

    /**
     * Since this target can be used for loading multiple images into a single TextView, we can't use the default
     * implementation which supports only one request per view. On the other hand, by using field to store the request
     * we lose the ability to clear previous requests if the client creates new instance of the
     * WPRemoteResourceViewTarget for the new request on the same view. Canceling any previous requests for the same
     * View must be handled by the client (see WPCustomImageGetter.clear(..) methods for reference).
     */
    override fun getRequest(): Request? {
        return request
    }

    override fun setRequest(request: Request?) {
        this.request = request
    }

    /**
     * We don't want to call super, since it determines the size from the size of the View. But this target may be used
     * for loading multiple images into a single View.
     */
    @SuppressLint("MissingSuperCall")
    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(maxSize, Target.SIZE_ORIGINAL)
    }

    /**
     * Drawable wrapper so we can replace placeholder with remote/error resource, after the requests finishes.
     *
     * We need to synchronously return drawable in WPCustomImageGetter.getDrawable(...).
     * If we return regular drawable - let's say a placeholder, we won't be able to replace it with the actual image
     * ==> This wrapper just adds us ability to change the content of the drawable after the asynchronous call finishes.
     */
    private class RemoteDrawableWrapper : Drawable() {
        internal var drawable: Drawable? = null

        fun setDrawable(drawable: Drawable) {
            this.drawable = drawable
        }

        override fun draw(canvas: Canvas) {
            drawable?.draw(canvas)
        }

        override fun setAlpha(alpha: Int) {
            drawable?.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            drawable?.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return drawable?.opacity ?: PixelFormat.UNKNOWN
        }

        override fun setBounds(bounds: Rect) {
            super.setBounds(bounds)
            drawable?.bounds = bounds
        }
    }
}
