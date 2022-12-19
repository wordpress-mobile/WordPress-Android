package org.wordpress.android.ui.avatars

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.AVATAR
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.TRAILING_LABEL
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class TrainOfAvatarsAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : Adapter<TrainOfAvatarsViewHolder<*>>() {
    private var itemsList = listOf<TrainOfAvatarsItem>()

    fun loadData(items: List<TrainOfAvatarsItem>) {
        val diffResult = DiffUtil.calculateDiff(
                TrainOfAvatarsAdapterDiffCallback(itemsList, items)
        )
        itemsList = items
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainOfAvatarsViewHolder<*> {
        return when (viewType) {
            AVATAR.ordinal -> AvatarViewHolder(parent, imageManager)
            TRAILING_LABEL.ordinal -> TrailingLabelViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Illegal view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: TrainOfAvatarsViewHolder<*>, position: Int) {
        when (val item = itemsList[position]) {
            is AvatarItem -> (holder as AvatarViewHolder).bind(item)
            is TrailingLabelTextItem -> (holder as TrailingLabelViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    override fun getItemViewType(position: Int): Int {
        return itemsList[position].type.ordinal
    }
}
