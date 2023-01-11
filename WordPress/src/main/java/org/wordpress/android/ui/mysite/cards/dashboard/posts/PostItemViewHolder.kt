package org.wordpress.android.ui.mysite.cards.dashboard.posts

import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PostItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO_ROUNDED_CORNERS

class PostItemViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<PostItemBinding>(
    parent.viewBinding(PostItemBinding::inflate)
) {
    fun bind(postItem: PostItem) = with(binding) {
        uiHelpers.setTextOrHide(title, postItem.title)
        uiHelpers.setTextOrHide(excerpt, postItem.excerpt)
        imageManager.loadImageWithCorners(
            featuredImage,
            PHOTO_ROUNDED_CORNERS,
            postItem.featuredImageUrl ?: "",
            uiHelpers.getPxOfUiDimen(
                WordPress.getContext(),
                UIDimenRes(R.dimen.my_site_post_item_image_corner_radius)
            )
        )
        featuredImage.setVisible(postItem.featuredImageUrl != null)
        iconTime.setVisible(postItem.isTimeIconVisible)
        itemView.setOnClickListener { postItem.onClick.click() }
    }
}
