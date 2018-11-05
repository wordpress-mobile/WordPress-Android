package org.wordpress.android.ui.sitecreation.segments

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentViewHolder.SegmentViewHolder
import org.wordpress.android.util.image.ImageManager

class NewSiteCreationSegmentsAdapter(
    private val onItemTapped: (VerticalSegmentModel) -> Unit = { },
    private val imageManager: ImageManager
) : Adapter<NewSiteCreationSegmentViewHolder>() {
    private val items = mutableListOf<VerticalSegmentModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SegmentViewHolder {
        return SegmentViewHolder(parent, imageManager, onItemTapped)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: NewSiteCreationSegmentViewHolder, position: Int) {
        holder.onBind(items[position], isLast = position == items.size - 1)
    }

    fun update(newItems: List<VerticalSegmentModel>) {
        val diffResult = DiffUtil.calculateDiff(SegmentsDiffUtils(items.toList(), newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SegmentsDiffUtils(
        val oldItems: List<VerticalSegmentModel>,
        val newItems: List<VerticalSegmentModel>
    ) :
            DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem.segmentId == newItem.segmentId
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
