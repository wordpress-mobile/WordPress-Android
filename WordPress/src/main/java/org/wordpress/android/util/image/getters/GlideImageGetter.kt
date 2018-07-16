package org.wordpress.android.util.image.getters

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.bumptech.glide.Glide
import org.wordpress.android.WordPress
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.R
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import java.lang.ref.WeakReference
import java.util.HashSet
import javax.inject.Inject

class GlideImageGetter(
    textView: TextView,
    private val maxWidth: Int
) : Html.ImageGetter, Drawable.Callback {
    private val textView: WeakReference<TextView> = WeakReference(textView)

    private val targets = HashSet<GlideRemoteResourceViewTarget>()

    @Inject lateinit var imageManager: ImageManager

    init {
        (WordPress.getContext() as WordPress).component().inject(this)
        clear(textView)
        textView.setTag(R.id.glide_image_loader_view_tag, this)
    }

    private fun clear(textView: TextView) {
        val prevGetter = textView.getTag(R.id.glide_image_loader_view_tag) as GlideImageGetter?
        prevGetter?.let {
            clear(textView.context, it.targets)
        }
        clear(textView.context, targets)
    }

    private fun clear(context: Context, targets: MutableSet<GlideRemoteResourceViewTarget>) {
        for (target in targets) {
            Glide.with(context).clear(target)
        }
        targets.clear()
    }

    override fun getDrawable(url: String): Drawable? {
        var source = url

        // images in reader comments may skip "http:" (no idea why) so make sure to add protocol here
        if (source.startsWith("//")) {
            source = "http:$source"
        }

        source = if (maxWidth > 0) PhotonUtils.getPhotonImageUrl(url, maxWidth, 0) else url

        return textView.get()?.let {
            val target = GlideRemoteResourceViewTarget(it, maxWidth)
            imageManager.load(target, ImageType.UNKNOWN_DIMENSIONS, source)
            targets.add(target)

            return target.drawable
        } ?: return null
    }

    override fun invalidateDrawable(who: Drawable) {
        textView.get()?.invalidate()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {}
}
