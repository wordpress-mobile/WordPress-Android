package org.wordpress.android.ui.comments.unified

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import org.wordpress.android.ui.comments.CommentListViewHolder
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.SUB_HEADER
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class UnifiedCommentListAdapter(
    val imageManager: ImageManager,
    val uiHelpers: UiHelpers
) : PagingDataAdapter<UnifiedCommentListItem, CommentListViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentListViewHolder {
        return when (viewType) {
            SUB_HEADER.ordinal -> UnifiedCommentSubHeaderViewHolder(parent)
            COMMENT.ordinal -> UnifiedCommentViewHolder(parent, imageManager, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view holder in UnifiedCommentListAdapter")
        }
    }

    override fun onBindViewHolder(holder: CommentListViewHolder, position: Int) {
        if (holder is UnifiedCommentSubHeaderViewHolder) {
            holder.bind((getItem(position) as SubHeader))
        } else if (holder is UnifiedCommentViewHolder) {
            holder.bind(getItem(position) as Comment)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)!!.type.ordinal
    }

    companion object {
        private val diffCallback = UnifiedCommentsListDiffCallback()
    }
}
