package org.wordpress.android.ui.mysite.cards.post

import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePostCardCreateFirstBinding
import org.wordpress.android.databinding.MySitePostCardDraftOrScheduledBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardCreateFirst
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardDraftOrScheduled
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.viewBinding

sealed class PostCardViewHolder<T : ViewBinding>(
    override val binding: T
) : MySiteCardAndItemViewHolder<T>(binding) {
    abstract fun bind(card: PostCard)

    class CreateFirst(
        parent: ViewGroup,
        private val imageManager: ImageManager,
        private val uiHelpers: UiHelpers
    ) : PostCardViewHolder<MySitePostCardCreateFirstBinding>(
            parent.viewBinding(MySitePostCardCreateFirstBinding::inflate)
    ) {
        override fun bind(card: PostCard) = with(binding) {
            val createFirstPostCard = card as PostCardCreateFirst
            uiHelpers.setTextOrHide(title, createFirstPostCard.title)
            uiHelpers.setTextOrHide(excerpt, createFirstPostCard.excerpt)
            imageManager.load(image, createFirstPostCard.imageRes)
        }
    }

    class DraftOrScheduled(
        parent: ViewGroup,
        imageManager: ImageManager,
        private val uiHelpers: UiHelpers
    ) : PostCardViewHolder<MySitePostCardDraftOrScheduledBinding>(
            parent.viewBinding(MySitePostCardDraftOrScheduledBinding::inflate)
    ) {
        init {
            binding.postItems.adapter = PostItemsAdapter(imageManager, uiHelpers)
        }

        override fun bind(card: PostCard) = with(binding) {
            val draftOrScheduledPostCard = card as PostCardDraftOrScheduled
            mySiteToolbar.update(draftOrScheduledPostCard.title)
            (postItems.adapter as PostItemsAdapter).update(draftOrScheduledPostCard.postItems)
        }

        private fun MySiteCardToolbarBinding.update(title: UiString?) {
            uiHelpers.setTextOrHide(mySiteCardToolbarTitle, title)
        }
    }
}
