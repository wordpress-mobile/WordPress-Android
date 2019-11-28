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

/**
 * ImageGetter for Html.fromHtml(). Retrieves images for HTML img tags using Glide library.
 *
 *
 * See {@link android.text.Html} for more details.
 */
class WPCustomImageGetter(
    textView: TextView,
    private val maxWidth: Int
) : Html.ImageGetter {
    private val textView: WeakReference<TextView> = WeakReference(textView)

    /**
     * We store all targets, so we can cancel any pending/ongoing requests when we want to load other content
     * into the TextView.
     */
    private val targets = HashSet<WPRemoteResourceViewTarget>()

    @Inject lateinit var imageManager: ImageManager

    init {
        (WordPress.getContext().applicationContext as WordPress).component().inject(this)
        clear(textView)
        // store the WPCustomImageGetter into the textView's tag, so we can cancel any pending/ongoing requests when the
        // TextView is reused.
        textView.setTag(R.id.glide_image_loader_view_tag, this)
    }

    /**
     * Cancels all pending/ongoing requests.
     */
    private fun clear(textView: TextView) {
        val prevGetter = textView.getTag(R.id.glide_image_loader_view_tag) as WPCustomImageGetter?
        prevGetter?.let {
            clear(textView.context, it.targets)
        }
        clear(textView.context, targets)
    }

    private fun clear(context: Context, targets: MutableSet<WPRemoteResourceViewTarget>) {
        for (target in targets) {
            Glide.with(context).clear(target)
        }
        targets.clear()
    }

    /**
     * This method is called when the HTML parser encounters an
     * img tag.
     */
    override fun getDrawable(url: String): Drawable? {
        var source = url

        // images in reader comments may skip "http:" (no idea why) so make sure to add protocol here
        if (source.startsWith("//")) {
            source = "http:$source"
        }

        source = if (maxWidth > 0) PhotonUtils.getPhotonImageUrl(url, maxWidth, 0) else url

        return textView.get()?.let {
            val target = WPRemoteResourceViewTarget(it, maxWidth)
            imageManager.loadIntoCustomTarget(target, ImageType.UNKNOWN, source)
            targets.add(target)

            target.drawable
        }
    }
}
