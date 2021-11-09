package org.wordpress.android.ui.mysite.cards.post

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePostCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardDraftOrScheduled
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.viewBinding

class PostCardViewHolder(
    parent: ViewGroup,
    imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySitePostCardBinding>(
        parent.viewBinding(MySitePostCardBinding::inflate)
) {
    init {
        binding.postItems.adapter = PostItemsAdapter(imageManager, uiHelpers)
    }

    fun bind(card: PostCard) = with(binding) {
        mySiteToolbar.update(card)
        (postItems.adapter as PostItemsAdapter).update((card as PostCardDraftOrScheduled).postItems)
    }

    private fun MySiteCardToolbarBinding.update(card: PostCard) {
        mySiteCardToolbarTitle.text = uiHelpers.getTextOfUiString(
                itemView.context,
                (card as PostCardDraftOrScheduled).title
        )
    }
}
