package org.wordpress.android.ui.comments.unified

import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import org.wordpress.android.R.attr
import org.wordpress.android.R.integer
import org.wordpress.android.databinding.CommentListItemBinding
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.util.viewBinding

class UnifiedCommentViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : UnifiedCommentListViewHolder<CommentListItemBinding>(parent.viewBinding(CommentListItemBinding::inflate)) {
    fun bind(item: Comment) = with(binding) {
        title.text = item.authorName
        comment.text = item.body

        uiHelpers.updateVisibility(imageCheckmark, item.isSelected)

        if (item.isSelected) {
            imageManager.cancelRequestAndClearImageView(avatar)
            layoutContainer.setBackgroundColor(
                    ColorUtils.setAlphaComponent(
                            layoutContainer.context.getColorFromAttribute(attr.colorOnSurface),
                            layoutContainer.context.resources.getInteger(integer.selected_list_item_opacity)
                    )
            )
        } else {
            imageManager.loadIntoCircle(avatar, AVATAR_WITH_BACKGROUND, item.avatarUrl)
            layoutContainer.background = null
        }

        uiHelpers.updateVisibility(statusIndicator, item.isPending)

        itemView.setOnClickListener {
            item.clickAction.onClick()
        }
        itemView.setOnLongClickListener {
            item.toggleAction.onToggle()
            true
        }
    }
}
