package org.wordpress.android.ui.posts

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.posts.PostListItemAction
import org.wordpress.android.viewmodel.posts.PostListItemAction.MoreItem
import org.wordpress.android.viewmodel.posts.PostListItemAction.SingleItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.WPTextView

class PostListItemViewHolder(
    parent: ViewGroup,
    config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : PostListItemSharedViewHolder(R.layout.post_list_item, parent, config, uiHelpers) {
    private val excerptTextView: WPTextView = itemView.findViewById(R.id.excerpt)
    private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
    private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
    private val actionButtons: List<PostListButton> = listOf(
            itemView.findViewById(R.id.btn_primary),
            itemView.findViewById(R.id.btn_secondary),
            itemView.findViewById(R.id.btn_ternary)
    )

    fun onBind(item: PostListItemUiState) {
        setBasicValues(item.data)

        uiHelpers.setTextOrHide(excerptTextView, item.data.excerpt)
        uiHelpers.updateVisibility(uploadProgressBar, item.data.showProgress)
        uiHelpers.updateVisibility(disabledOverlay, item.data.showOverlay)
        itemView.setOnClickListener { item.onSelected.invoke() }

        actionButtons.forEachIndexed { index, button ->
            updateMenuItem(button, item.actions.getOrNull(index))
        }
    }

    private fun updateMenuItem(postListButton: PostListButton, action: PostListItemAction?) {
        uiHelpers.updateVisibility(postListButton, action != null)
        if (action != null) {
            when (action) {
                is SingleItem -> {
                    postListButton.updateButtonType(action.buttonType)
                    postListButton.setOnClickListener { action.onButtonClicked.invoke(action.buttonType) }
                }
                is MoreItem -> {
                    postListButton.updateButtonType(action.buttonType)
                    postListButton.setOnClickListener { view ->
                        action.onButtonClicked.invoke(action.buttonType)
                        onMoreClicked(action.actions, view)
                    }
                }
            }
        }
    }
}
