package org.wordpress.android.ui.reader.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.LikerFaceItemBinding
import org.wordpress.android.ui.reader.adapters.FACE_ITEM_AVATAR_SIZE_DIMEN
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.FaceItem
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.viewBinding

class PostLikerViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager
) : TrainOfFacesViewHolder<LikerFaceItemBinding>(parent.viewBinding(LikerFaceItemBinding::inflate)) {
    fun bind(liker: FaceItem) = with(binding) {
        val likerAvatarUrl = GravatarUtils.fixGravatarUrl(
                liker.userAvatarUrl,
                itemView.context.resources.getDimensionPixelSize(FACE_ITEM_AVATAR_SIZE_DIMEN)
        )

        this.likerAvatar.setOnClickListener(null)
        imageManager.loadIntoCircle(this.likerAvatar, ImageType.AVATAR_WITH_BACKGROUND, likerAvatarUrl)
    }
}
