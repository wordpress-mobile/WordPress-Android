package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.StatsBlockListItemWithImageBinding
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithImage
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.IMAGE

private const val SITE_IMAGE_CORNER_RADIUS_IN_DP = 4

class ListItemWithImageViewHolder(
    parent: ViewGroup,
    val binding: StatsBlockListItemWithImageBinding = parent.viewBinding(StatsBlockListItemWithImageBinding::inflate),
    val imageManager: ImageManager
) : BlockListItemViewHolder(
        binding.root
) {
    fun bind(
        item: ListItemWithImage
    ) = with(binding) {
        statsTitle.text = item.title
        statsSubTitle.text = item.subTitle
        item.imageUrl?.let {
            statsImage.visibility = View.VISIBLE
            imageManager.loadImageWithCorners(
                    statsImage,
                    IMAGE,
                    it,
                    DisplayUtils.dpToPx(statsImage.context, SITE_IMAGE_CORNER_RADIUS_IN_DP)
            )
        }
    }
}
