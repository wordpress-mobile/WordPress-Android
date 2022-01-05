package org.wordpress.android.ui.reader.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState

abstract class CommentsSnippetViewHolder<T : ViewBinding>(
    internal val parent: ViewGroup,
    inflateBinding: (LayoutInflater, ViewGroup, Boolean) -> T,
    protected val binding: T = inflateBinding(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {
    abstract fun onBind(itemUiState: CommentSnippetItemState)
}
