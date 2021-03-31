package org.wordpress.android.ui.reader.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.engagement.EngageItem
import org.wordpress.android.ui.engagement.EngageItem.EngageItemType.LIKER
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagedPeopleAdapterDiffCallback
import org.wordpress.android.ui.engagement.EngagedPeopleViewHolder
import org.wordpress.android.ui.reader.viewholders.PostLikerViewHolder
import org.wordpress.android.util.image.ImageManager

class ReaderPostLikersAdapter(
    private val imageManager: ImageManager,
    private val context: Context
) : Adapter<EngagedPeopleViewHolder>() {
    private var itemsList = listOf<EngageItem>()

    fun loadData(items: List<EngageItem>) {
        val diffResult = DiffUtil.calculateDiff(
                EngagedPeopleAdapterDiffCallback(itemsList, items)
        )
        itemsList = items
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EngagedPeopleViewHolder {
        return when (viewType) {
            LIKER.ordinal -> PostLikerViewHolder(parent, imageManager, context)
            else -> throw IllegalArgumentException("Illegal view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: EngagedPeopleViewHolder, position: Int) {
        val item = itemsList[position]
        when (item) {
            is Liker -> (holder as PostLikerViewHolder).bind(item)
            else -> throw IllegalArgumentException("Illegal EngageItem $item")
        }
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    override fun getItemViewType(position: Int): Int {
        return itemsList[position].type.ordinal
    }
}
