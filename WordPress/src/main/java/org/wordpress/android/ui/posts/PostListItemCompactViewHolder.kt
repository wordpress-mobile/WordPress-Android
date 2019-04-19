package org.wordpress.android.ui.posts

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState

class PostListItemCompactViewHolder(
    parent: ViewGroup,
    config: PostViewHolderConfig,
    uiHelpers: UiHelpers
) : PostListItemSharedViewHolder(R.layout.post_list_item_compact, parent, config, uiHelpers) {
    private val moreButton: ImageButton = itemView.findViewById(R.id.more_button)

    private var currentItem: PostListItemUiState? = null

    init {
        itemView.setOnClickListener { currentItem?.onSelected?.invoke() }
        moreButton.setOnClickListener { handleOpenMenuClick(it) }
    }

    fun bind(item: PostListItemUiState) {
        currentItem = item
        setBasicValues(item.compactData)

        itemView.setOnClickListener { item.onSelected.invoke() }
        moreButton.setOnClickListener { onMoreClicked(item.compactActions, moreButton) }
    }

    private fun handleOpenMenuClick(view: View) {
        currentItem?.compactActions?.let { actions ->
            onMoreClicked(actions, view)
        }
    }
}
