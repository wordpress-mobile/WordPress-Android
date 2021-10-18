package org.wordpress.android.ui.mysite.cards.post

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePostCardBinding
import org.wordpress.android.databinding.PostCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class PostCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySitePostCardBinding>(
        parent.viewBinding(MySitePostCardBinding::inflate)
) {
    fun bind(card: PostCard) = with(binding) {
        mySiteToolbar.update(card)
        postCard.update(card)
    }

    private fun MySiteCardToolbarBinding.update(card: PostCard) {
        mySiteCardToolbarTitle.text = uiHelpers.getTextOfUiString(itemView.context, card.title)
    }

    private fun PostCardBinding.update(card: PostCard) {
        postCardTitle.text = uiHelpers.getTextOfUiString(itemView.context, card.postTitle)
    }
}
