package org.wordpress.android.ui.posts

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.databinding.PostListItemCompactBinding
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState

class PostListItemCompactViewHolder(
    private val binding: PostListItemCompactBinding,
    private val config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PostListItemUiState) {
        //TODO: Setup this row from the UI state object
    }

    companion object {
        @JvmStatic
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            config: PostViewHolderConfig,
            uiHelpers: UiHelpers
        ): PostListItemCompactViewHolder {
            val binding = PostListItemCompactBinding.inflate(inflater, parent, false)
            return PostListItemCompactViewHolder(binding, config, uiHelpers)
        }
    }
}