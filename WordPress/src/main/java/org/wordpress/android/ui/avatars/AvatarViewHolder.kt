package org.wordpress.android.ui.avatars

import android.view.ViewGroup
import org.wordpress.android.databinding.AvatarItemBinding
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

class AvatarViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager
) : TrainOfAvatarsViewHolder<AvatarItemBinding>(parent.viewBinding(AvatarItemBinding::inflate)) {
    fun bind(avatarDetails: AvatarItem) = with(binding) {
        val likerAvatarUrl = GravatarUtils.fixGravatarUrl(
            avatarDetails.userAvatarUrl,
            itemView.context.resources.getDimensionPixelSize(AVATAR_SIZE_DIMEN)
        )

        this.avatar.setOnClickListener(null)
        this.avatar.isClickable = false
        imageManager.loadIntoCircle(this.avatar, ImageType.AVATAR_WITH_BACKGROUND, likerAvatarUrl)
    }
}
