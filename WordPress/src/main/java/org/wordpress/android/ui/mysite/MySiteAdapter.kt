package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.Type.HEADER
import org.wordpress.android.ui.mysite.MySiteItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_ACTIONS_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_BLOCK
import org.wordpress.android.util.image.ImageManager

class MySiteAdapter(val imageManager: ImageManager) : Adapter<MySiteItemViewHolder>() {
    private var items = listOf<MySiteItem>()
    fun loadData(result: List<MySiteItem>) {
        val diffResult = DiffUtil.calculateDiff(
                MySiteAdapterDiffCallback(items, result)
        )
        items = result
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySiteItemViewHolder {
        return when (viewType) {
            SITE_INFO_BLOCK.ordinal -> MySiteInfoViewHolder(parent, imageManager)
            QUICK_ACTIONS_BLOCK.ordinal -> QuickActionsViewHolder(parent)
            HEADER.ordinal -> TODO()
            LIST_ITEM.ordinal -> TODO()
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: MySiteItemViewHolder, position: Int) {
        when (holder) {
            is MySiteInfoViewHolder -> holder.bind(items[position] as SiteInfoBlock)
            is QuickActionsViewHolder -> holder.bind(items[position] as QuickActionsBlock)
        }
    }

    override fun getItemViewType(position: Int) = items[position].type.ordinal

    override fun getItemCount(): Int = items.size
}
