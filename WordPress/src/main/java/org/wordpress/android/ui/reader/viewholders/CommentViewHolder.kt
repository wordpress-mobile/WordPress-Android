package org.wordpress.android.ui.reader.viewholders

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderListitemCommentBinding
import org.wordpress.android.ui.comments.CommentUtils
import org.wordpress.android.ui.reader.utils.ThreadedCommentsUtils
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState.CommentState
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

class CommentViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val threadedCommentsUtils: ThreadedCommentsUtils
) : CommentsSnippetViewHolder<ReaderListitemCommentBinding>(
    parent,
    ReaderListitemCommentBinding::inflate
) {
    override fun onBind(itemUiState: CommentSnippetItemState) = with(binding) {
        val state = itemUiState as CommentState

        actionsContainer.visibility = View.GONE
        divider.visibility = View.GONE

        val readerSpacing = itemView.resources.getDimensionPixelSize(R.dimen.reader_detail_margin)

        val params = commentBodyContainer.layoutParams as MarginLayoutParams
        params.marginStart = readerSpacing
        params.marginEnd = readerSpacing
        commentBodyContainer.layoutParams = params

        textCommentAuthor.text = state.authorName
        textCommentDate.text = state.datePublished
        @Suppress("DEPRECATION")
        imageManager.loadIntoCircle(imageCommentAvatar, ImageType.AVATAR, state.avatarUrl)
        authorBadge.visibility = if (state.showAuthorBadge) View.VISIBLE else View.GONE
        commentActionButtonContainer.visibility = View.GONE

        threadedCommentsUtils.setLinksClickable(textCommentText, state.isPrivatePost)
        CommentUtils.displayHtmlComment(
            textCommentText,
            state.commentText,
            threadedCommentsUtils.getMaxWidthForContent(),
            textCommentText.lineHeight,
            itemView.resources.getString(R.string.comment_unable_to_show_error)
        )
    }
}
