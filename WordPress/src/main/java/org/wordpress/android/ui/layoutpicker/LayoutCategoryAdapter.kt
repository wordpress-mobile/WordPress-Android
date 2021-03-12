package org.wordpress.android.ui.layoutpicker

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter

/**
 * Renders the layout categories
 */
class LayoutCategoryAdapter : Adapter<LayoutsItemViewHolder>() {
    private var items: List<LayoutCategoryUiState> = listOf()

    fun update(newItems: List<LayoutCategoryUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                LayoutCategoryDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LayoutsItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LayoutsItemViewHolder(parent)
}
