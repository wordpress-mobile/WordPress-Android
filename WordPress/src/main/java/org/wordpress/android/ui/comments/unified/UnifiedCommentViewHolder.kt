package org.wordpress.android.ui.comments.unified

import android.text.TextUtils
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.databinding.CommentListItemBinding
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.utils.AnimationUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.viewmodel.ResourceProvider

@Suppress("LongParameterList")
class UnifiedCommentViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    private val commentListUiUtils: CommentListUiUtils,
    private val resourceProvider: ResourceProvider,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
    private val animationUtilsWrapper: AnimationUtilsWrapper
) : UnifiedCommentListViewHolder<CommentListItemBinding>(parent.viewBinding(CommentListItemBinding::inflate)) {
    fun bind(item: Comment) = with(binding) {
        title.text = commentListUiUtils.formatCommentTitle(item.authorName, item.postTitle, title.context)
        comment.post { // we need to know the width of the view for image loading before rendering content
            commentListUiUtils.displayHtmlComment(item.content, comment, comment.width, comment.lineHeight)
        }

        if (item.isSelected) {
            imageManager.cancelRequestAndClearImageView(avatar)
        } else {
            imageManager.loadIntoCircle(
                avatar,
                AVATAR_WITH_BACKGROUND,
                getGravatarUrl(item)
            )
        }

        uiHelpers.updateVisibility(imageCheckmark, item.isSelected)
        commentListUiUtils.toggleSelectedStateOfCommentListItem(layoutContainer, item.isSelected)

        uiHelpers.updateVisibility(statusIndicator, item.isPending)

        itemView.setOnClickListener {
            item.clickAction.onClick()
        }
        itemView.setOnLongClickListener {
            item.toggleAction.onToggle()
            true
        }
    }

    fun toggleSelected(isSelected: Boolean) = with(binding) {
        animationUtilsWrapper.startAnimation(
            imageCheckmark,
            if (isSelected) R.anim.comment_multiselect_checkbox_in else R.anim.comment_multiselect_checkbox_out
        )
        uiHelpers.updateVisibility(imageCheckmark, isSelected)
        commentListUiUtils.toggleSelectedStateOfCommentListItem(layoutContainer, isSelected)
    }

    fun updateStateAndListeners(item: Comment) = with(binding) {
        uiHelpers.updateVisibility(statusIndicator, item.isPending)
        itemView.setOnClickListener {
            item.clickAction.onClick()
        }
        itemView.setOnLongClickListener {
            item.toggleAction.onToggle()
            true
        }
    }

    private fun getGravatarUrl(comment: Comment): String {
        return if (!TextUtils.isEmpty(comment.authorAvatarUrl)) {
            gravatarUtilsWrapper.fixGravatarUrl(
                comment.authorAvatarUrl,
                resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_medium)
            )
        } else if (!TextUtils.isEmpty(comment.authorEmail)) {
            GravatarUtils.gravatarFromEmail(
                comment.authorEmail,
                resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_medium)
            )
        } else {
            ""
        }
    }
}
