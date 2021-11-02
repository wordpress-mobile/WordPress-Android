package org.wordpress.android.ui.mysite.cards.post

import android.view.ViewGroup
import org.wordpress.android.databinding.PostItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class PostItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<PostItemBinding>(
        parent.viewBinding(PostItemBinding::inflate)
) {
    fun bind(postItem: PostItem) = with(binding) {
        postCardTitle.text = uiHelpers.getTextOfUiString(itemView.context, postItem.postTitle)
    }
}

