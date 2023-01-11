package org.wordpress.android.ui.comments.unified

import android.view.ViewGroup
import org.wordpress.android.databinding.CommentListSubheaderBinding
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.util.extensions.viewBinding

class UnifiedCommentSubHeaderViewHolder(
    parent: ViewGroup
) : UnifiedCommentListViewHolder<CommentListSubheaderBinding>(
    parent.viewBinding(CommentListSubheaderBinding::inflate)
) {
    fun bind(item: SubHeader) = with(binding) {
        label.text = item.label
    }
}
