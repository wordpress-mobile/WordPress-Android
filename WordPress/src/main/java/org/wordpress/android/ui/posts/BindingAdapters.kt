package org.wordpress.android.ui.posts

import android.databinding.BindingAdapter
import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.widgets.WPTextView

@BindingAdapter("visible")
fun bindViewVisible(view: View, visible: Boolean) {
    val visibility = if (visible) {
        View.VISIBLE
    } else {
        View.GONE
    }
    view.visibility = visibility
}

@BindingAdapter("postsImage")
fun bindPostsImage(imageView: ImageView, bundle: ImageBundle) {
    bundle.url?.let { url ->
        val config = bundle.config
        if (url.startsWith("http")) {
            val photonUrl = ReaderUtils.getResizedImageUrl(
                    url, config.photonWidth, config.photonHeight, !config.isPhotonCapable
            )
            config.imageManager.load(imageView, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP)
        } else {
            ImageUtils.getWPImageSpanThumbnailFromFilePath(
                    imageView.context, url, config.photonWidth
            )?.let {
                config.imageManager.load(imageView, it)
            }
        }
    } ?: bundle.config.imageManager.cancelRequestAndClearImageView(imageView)
}

@BindingAdapter("uiStringValue")
fun bindUiStringValue(textView: WPTextView, value: UiString) {
    val text = when (value) {
        is UiString.UiStringRes -> textView.context.getString(value.stringRes)
        is UiString.UiStringText -> value.text
    }
    textView.text = text
}
