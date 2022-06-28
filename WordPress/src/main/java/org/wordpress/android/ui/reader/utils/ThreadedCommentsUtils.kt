package org.wordpress.android.ui.reader.utils

import android.widget.TextView
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@Reusable
class ThreadedCommentsUtils @Inject constructor(
    val contextProvider: ContextProvider
) {
    fun getMaxWidthForContent(): Int {
        val context = contextProvider.getContext()

        // calculate the max width of comment content
        val displayWidth = DisplayUtils.getWindowPixelWidth(context)
        val cardMargin: Int = context.resources.getDimensionPixelSize(R.dimen.reader_card_margin)
        val contentPadding: Int = context.resources.getDimensionPixelSize(R.dimen.reader_card_content_padding)
        val mediumMargin: Int = context.resources.getDimensionPixelSize(R.dimen.margin_medium)

        return displayWidth - cardMargin * 2 - contentPadding * 2 - mediumMargin * 2
    }

    fun isPrivatePost(post: ReaderPost?) = post != null && post.isPrivate

    fun setLinksClickable(textView: TextView?, isPrivatePost: Boolean) {
        textView?.let {
            it.linksClickable = true
            it.movementMethod = ReaderLinkMovementMethod.getInstance(isPrivatePost)
        }
    }
}
