package org.wordpress.android.ui.reader.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ReaderListitemCommentLoadingBinding
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState

class CommentsLoadingViewHolder(
    parent: ViewGroup
) : CommentsSnippetViewHolder<ReaderListitemCommentLoadingBinding>(
    parent,
    ReaderListitemCommentLoadingBinding::inflate
) {
    override fun onBind(itemUiState: CommentSnippetItemState) = with(binding) {
        // nothing to do actually
    }
}
