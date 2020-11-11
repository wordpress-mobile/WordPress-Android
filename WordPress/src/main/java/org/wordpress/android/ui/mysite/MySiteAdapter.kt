package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter

class MySiteAdapter : Adapter<MySiteItemViewHolder>() {
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
            MySiteItem.Type.SITE_BLOCK.ordinal -> TODO()
            MySiteItem.Type.HEADER.ordinal -> TODO()
            MySiteItem.Type.LIST_ITEM.ordinal -> TODO()
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: MySiteItemViewHolder, position: Int) {
        TODO()
    }

    override fun getItemCount(): Int = items.size
}
