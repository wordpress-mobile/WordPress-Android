package org.wordpress.android.ui.comments.unified

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.R.integer
import org.wordpress.android.ui.comments.CommentListViewHolder
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND

class UnifiedCommentViewHolder(
    parent: ViewGroup,
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

    fun bind(item: Comment) {
        title.text = item.authorName
        commentBody.text = item.body

        uiHelpers.updateVisibility(checkMark, item.isSelected)

        if (item.isSelected) {
            imageManager.cancelRequestAndClearImageView(avatar)
            containerView.setBackgroundColor(
                    ColorUtils.setAlphaComponent(
                            containerView.context.getColorFromAttribute(attr.colorOnSurface),
                            containerView.context.resources.getInteger(integer.selected_list_item_opacity)
                    )
            )
        } else {
            imageManager.loadIntoCircle(avatar, AVATAR_WITH_BACKGROUND, item.avatarUrl)
            containerView.background = null
        }

        uiHelpers.updateVisibility(statusIndicator, item.isPending)

        itemView.setOnClickListener {
            item.clickAction.click()
        }
        itemView.setOnLongClickListener {
            item.toggleAction.toggle()
            true
        }
    }
}
