package org.wordpress.android.ui.reader.viewholders

import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.databinding.BloggerLikersTextItemBinding
import org.wordpress.android.ui.reader.adapters.FACE_ITEM_AVATAR_SIZE_DIMEN
import org.wordpress.android.ui.reader.adapters.FACE_ITEM_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.BloggersLikingTextItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.viewBinding

class BloggingLikersTextViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : TrainOfFacesViewHolder<BloggerLikersTextItemBinding>(parent.viewBinding(BloggerLikersTextItemBinding::inflate)) {
    fun bind(bloggersTextItem: BloggersLikingTextItem) = with(binding) {
        val position = adapterPosition

        if (position >= 0) {
            val displayWidth = DisplayUtils.getDisplayPixelWidth(itemView.context)
            val paddingWidth = 2 * itemView.context.resources.getDimensionPixelSize(R.dimen.reader_detail_margin)
            val avatarSize = itemView.context.resources.getDimensionPixelSize(FACE_ITEM_AVATAR_SIZE_DIMEN)
            val leftOffest = itemView.context.resources.getDimensionPixelSize(FACE_ITEM_LEFT_OFFSET_DIMEN)
            val facesWidth = position * avatarSize - (position - 1).coerceAtLeast(0) * leftOffest

            itemView.layoutParams.width = displayWidth - paddingWidth - facesWidth
        }

        numBloggers.text = with(bloggersTextItem) {
            uiHelpers.getTextOfUiString(itemView.context, text)
        }
    }
}
