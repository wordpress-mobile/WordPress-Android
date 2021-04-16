package org.wordpress.android.ui.engagement

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.engagement.EngageItem.EngageItemType
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngageItem.NextLikesPageLoader
import org.wordpress.android.util.image.ImageManager
import java.lang.IllegalArgumentException

class EngagedPeopleAdapter constructor(
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
            EngageItemType.LIKED_ITEM.ordinal -> LikedItemViewHolder(parent, imageManager)
            EngageItemType.LIKER.ordinal -> LikerViewHolder(parent, imageManager)
            EngageItemType.NEXT_LIKES_PAGE_LOADER.ordinal -> NextPageLoadViewHolder(parent)
            else -> throw IllegalArgumentException("Illegal view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: EngagedPeopleViewHolder, position: Int) {
        val item = itemsList[position]
        when (item) {
            is LikedItem -> (holder as LikedItemViewHolder).bind(item)
            is Liker -> (holder as LikerViewHolder).bind(item)
            is NextLikesPageLoader -> (holder as NextPageLoadViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    override fun getItemViewType(position: Int): Int {
        return itemsList[position].type.ordinal
    }
}
