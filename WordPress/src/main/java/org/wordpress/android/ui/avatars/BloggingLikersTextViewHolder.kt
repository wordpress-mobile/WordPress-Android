package org.wordpress.android.ui.avatars

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.BloggerLikersTextItemBinding
import org.wordpress.android.ui.avatars.TrainOfFacesItem.BloggersLikingTextItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class BloggingLikersTextViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : TrainOfFacesViewHolder<BloggerLikersTextItemBinding>(parent.viewBinding(BloggerLikersTextItemBinding::inflate)) {
    fun bind(bloggersTextItem: BloggersLikingTextItem) = with(binding) {
        val position = adapterPosition

        if (position >= 0) {
            itemView.post {
                val parent = findParentRecyclerView(itemView)

                parent?.let {
                    val parentWidth = it.measuredWidth
                    val avatarSize = itemView.context.resources.getDimensionPixelSize(FACE_ITEM_AVATAR_SIZE_DIMEN)
                    val leftOffest = itemView.context.resources.getDimensionPixelSize(FACE_ITEM_LEFT_OFFSET_DIMEN)
                    val facesWidth = position * avatarSize - (position - 1).coerceAtLeast(0) * leftOffest

                    itemView.layoutParams.width = parentWidth - facesWidth
                }
            }
        }

        numBloggers.text = with(bloggersTextItem) {
            uiHelpers.getTextOfUiString(itemView.context, text)
        }
    }

    // we do assume that the adapter is only attached to one RecyclerView at time
    private fun findParentRecyclerView(v: View): View? {
        return when (val parent: ViewParent = v.parent) {
            is RecyclerView -> {
                parent
            }
            is View -> {
                findParentRecyclerView(parent as View)
            }
            else -> {
                null
            }
        }
    }
}
