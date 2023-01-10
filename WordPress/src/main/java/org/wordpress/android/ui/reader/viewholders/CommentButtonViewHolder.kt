package org.wordpress.android.ui.reader.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ReaderListitemCommentButtonBinding
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState.ButtonState
import org.wordpress.android.ui.utils.UiHelpers

class CommentButtonViewHolder(
    parent: ViewGroup,
    val uiHelpers: UiHelpers
) : CommentsSnippetViewHolder<ReaderListitemCommentButtonBinding>(
    parent,
    ReaderListitemCommentButtonBinding::inflate
) {
    override fun onBind(itemUiState: CommentSnippetItemState) = with(binding) {
        val state = itemUiState as ButtonState

        commentActionButton.text = uiHelpers.getTextOfUiString(itemView.context, state.buttonText)

        commentActionButton.setOnClickListener { state.onCommentSnippetClicked.invoke(state.postId, state.blogId) }
    }
}
