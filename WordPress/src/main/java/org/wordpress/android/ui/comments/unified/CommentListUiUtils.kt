package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.R.integer
import org.wordpress.android.R.string
import org.wordpress.android.ui.comments.CommentUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import javax.inject.Inject

class CommentListUiUtils @Inject constructor() {
    fun displayHtmlComment(commentContent: String, textView: TextView, maxImageWidth: Int, maxEmojiWidth: Int) {
        CommentUtils.displayHtmlComment(
            textView,
            commentContent,
            maxImageWidth,
            maxEmojiWidth,
            textView.resources.getString(R.string.comment_unable_to_show_error)
        )
    }

    fun formatCommentTitle(authorName: String, postTitle: String, context: Context): Spannable {
        val formattedTitle: String
        var author = context.getString(string.anonymous)
        if (!TextUtils.isEmpty(authorName)) {
            author = authorName.trim { it <= ' ' }
        }
        val trimmedPostTitle = postTitle.trim { it <= ' ' }
        formattedTitle = if (!TextUtils.isEmpty(postTitle)) {
            context.getString(string.comment_title, author, postTitle)
        } else {
            author
        }
        val string = SpannableStringBuilder(formattedTitle)
        val authorStartIndex = formattedTitle.indexOf(author)
        val authorEndIndex = authorStartIndex + author.length
        val titleStartIndex = formattedTitle.indexOf(trimmedPostTitle)
        val titleEndIndex = titleStartIndex + trimmedPostTitle.length
        string.setSpan(
            StyleSpan(Typeface.BOLD), authorStartIndex, authorEndIndex,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        string.setSpan(
            StyleSpan(Typeface.BOLD), titleStartIndex, titleEndIndex,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        return string
    }

    fun toggleSelectedStateOfCommentListItem(containerView: View, isSelected: Boolean) {
        if (isSelected) {
            containerView.setBackgroundColor(
                ColorUtils.setAlphaComponent(
                    containerView.context.getColorFromAttribute(attr.colorOnSurface),
                    containerView.context.resources.getInteger(integer.selected_list_item_opacity)
                )
            )
        } else {
            containerView.background = null
        }
    }
}
