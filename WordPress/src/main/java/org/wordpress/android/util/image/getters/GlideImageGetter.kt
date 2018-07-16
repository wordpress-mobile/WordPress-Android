package org.wordpress.android.util.image.getters

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.bumptech.glide.Glide
import org.wordpress.android.modules.GlideApp
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.R
import java.lang.ref.WeakReference
import java.util.HashSet

class GlideImageGetter(
    textView: TextView,
    private val mMaxWidth: Int,
    private val mPlaceholderDrawable: Drawable,
    private val mErrorDrawable: Drawable
) : Html.ImageGetter, Drawable.Callback {
    private val mTextView: WeakReference<TextView> = WeakReference(textView)

    private val mTargets = HashSet<GlideRemoteResourceViewTarget>()

    init {
        clear(textView)
        textView.setTag(R.id.glide_image_loader_view_tag, this)
    }

    private fun clear(textView: TextView) {
        val prevGetter = textView.getTag(R.id.glide_image_loader_view_tag) as GlideImageGetter?
        prevGetter?.let {
            clear(textView.context, it.mTargets)
        }
        clear(textView.context, mTargets)
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

        source = if (mMaxWidth > 0) PhotonUtils.getPhotonImageUrl(url, mMaxWidth, 0) else url

        return mTextView.get()?.let {
            val target = GlideRemoteResourceViewTarget(it, mMaxWidth)
            mTargets.add(GlideApp.with(it.context)
                    .load(source)
                    .placeholder(mPlaceholderDrawable)
                    .error(mErrorDrawable)
                    .into(target))

            return target.drawable
        } ?: return null
    }

    override fun invalidateDrawable(who: Drawable) {
        mTextView.get()?.invalidate()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {}
}
