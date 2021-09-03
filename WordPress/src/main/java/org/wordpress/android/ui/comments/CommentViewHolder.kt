package org.wordpress.android.ui.comments

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.R.integer
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.ui.comments.CommentAdapter.OnCommentPressedListener
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.WPHtml
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND

@Deprecated("Comments are being refactored as part of Comments Unification project. If you are adding any" +
        " features or modifying this class, please ping develric or klymyam")
class CommentViewHolder(
    parent: ViewGroup,
    private val clickListener: OnCommentPressedListener,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : CommentListViewHolder(
        parent,
        R.layout.comment_list_item
) {
    private val title: TextView = itemView.findViewById(R.id.title)
    private val commentBody: TextView = itemView.findViewById(R.id.comment)
    private val avatar: ImageView = itemView.findViewById(R.id.avatar)
    private val checkMark: ImageView = itemView.findViewById(R.id.image_checkmark)
    private val containerView: ViewGroup = itemView.findViewById(R.id.layout_container)
    private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)

    fun bind(item: CommentListItem.Comment, isSelected: Boolean) {
        val commentModel = item.comment

        title.text = getFormattedTitle(commentModel, title.context)
        commentBody.text = getSpannedContent(commentModel, commentBody.context)

        uiHelpers.updateVisibility(checkMark, isSelected)

        if (isSelected) {
            imageManager.cancelRequestAndClearImageView(avatar)
            containerView.setBackgroundColor(
                    ColorUtils.setAlphaComponent(
                            containerView.context.getColorFromAttribute(attr.colorOnSurface),
                            containerView.context.resources.getInteger(integer.selected_list_item_opacity)
                    )
            )
        } else {
            imageManager.loadIntoCircle(
                    avatar, AVATAR_WITH_BACKGROUND,
                    getAvatarForDisplay(commentModel, avatar.resources.getDimensionPixelSize(R.dimen.avatar_sz_medium))
            )
            containerView.background = null
        }

        uiHelpers.updateVisibility(statusIndicator, CommentStatus.fromString(commentModel.status) == UNAPPROVED)

        itemView.setOnClickListener {
            clickListener.onCommentPressed(adapterPosition, it)
        }
        itemView.setOnLongClickListener {
            clickListener.onCommentLongPressed(adapterPosition, it)
            true
        }
    }

    private fun getAvatarForDisplay(comment: CommentModel, avatarSize: Int): String {
        var avatarForDisplay = ""
        if (!TextUtils.isEmpty(comment.authorProfileImageUrl)) {
            avatarForDisplay = GravatarUtils.fixGravatarUrl(comment.authorProfileImageUrl, avatarSize)
        } else if (!TextUtils.isEmpty(comment.authorEmail)) {
            avatarForDisplay = GravatarUtils.gravatarFromEmail(comment.authorEmail, avatarSize)
        }
        return avatarForDisplay
    }

    private fun getSpannedContent(comment: CommentModel, context: Context): Spanned {
        val spannedContent = WPHtml.fromHtml(StringUtils.notNullStr(comment.content), null, null, context, null, 0)
        val trimmedContent = spannedContent.trim()
        return spannedContent.subSequence(0, trimmedContent.length) as Spanned
    }

    private fun getFormattedTitle(comment: CommentModel, context: Context): Spannable {
        val formattedTitle: String
        var author = context.getString(string.anonymous)
        if (!TextUtils.isEmpty(comment.authorName)) {
            author = comment.authorName.trim { it <= ' ' }
        }
        val postTitle = comment.postTitle.trim { it <= ' ' }
        formattedTitle = if (!TextUtils.isEmpty(comment.postTitle)) {
            context.getString(string.comment_title, author, postTitle)
        } else {
            author
        }
        val string = SpannableStringBuilder(formattedTitle)
        val authorStartIndex = formattedTitle.indexOf(author)
        val authorEndIndex = authorStartIndex + author.length
        val titleStartIndex = formattedTitle.indexOf(postTitle)
        val titleEndIndex = titleStartIndex + postTitle.length
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
}
