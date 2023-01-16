package org.wordpress.android.ui.comments.unified

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.LoadOrActionItemBinding
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.NextPageLoader
import org.wordpress.android.util.extensions.viewBinding

class LoadStateViewHolder(parent: ViewGroup) : UnifiedCommentListViewHolder<LoadOrActionItemBinding>(
    parent.viewBinding(LoadOrActionItemBinding::inflate)
) {
    fun bind(item: NextPageLoader) = with(binding) {
        if (!item.isLoading) {
            progress.visibility = View.GONE
            actionButton.visibility = View.VISIBLE
            actionButton.setOnClickListener {
                item.loadAction()
            }
        } else {
            item.loadAction()
            progress.visibility = View.VISIBLE
            actionButton.visibility = View.GONE
        }
    }
}
