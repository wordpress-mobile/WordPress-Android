package org.wordpress.android.ui.avatars

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.TrailingLabelItemBinding
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.viewBinding

class TrailingLabelViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : TrainOfAvatarsViewHolder<TrailingLabelItemBinding>(parent.viewBinding(TrailingLabelItemBinding::inflate)) {
    fun bind(textItem: TrailingLabelTextItem) = with(binding) {
        val position = adapterPosition

        if (position >= 0) {
            itemView.post {
                val parent = findParentRecyclerView(itemView)

                parent?.let {
                    val parentWidth = it.measuredWidth
                    val avatarSize = itemView.context.resources.getDimensionPixelSize(AVATAR_SIZE_DIMEN)
                    val leftOffset = itemView.context.resources.getDimensionPixelSize(AVATAR_LEFT_OFFSET_DIMEN)
                    val facesWidth = position * avatarSize - (position - 1).coerceAtLeast(0) * leftOffset

                    itemView.layoutParams.width = parentWidth - facesWidth
                }
            }
        }

        label.setTextColor(label.context.getColorFromAttribute(textItem.labelColor))

        label.text = with(textItem) {
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
