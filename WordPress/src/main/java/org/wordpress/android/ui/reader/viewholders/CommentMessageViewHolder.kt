package org.wordpress.android.ui.reader.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ReaderListitemCommentMessageBinding
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState.TextMessage
import org.wordpress.android.ui.utils.UiHelpers

class CommentMessageViewHolder(
    parent: ViewGroup,
    val uiHelpers: UiHelpers
) : CommentsSnippetViewHolder<ReaderListitemCommentMessageBinding>(
    parent,
    ReaderListitemCommentMessageBinding::inflate
) {
    override fun onBind(itemUiState: CommentSnippetItemState) = with(binding) {
        val state = itemUiState as TextMessage

        textEmpty.text = uiHelpers.getTextOfUiString(itemView.context, state.message)
    }
}
