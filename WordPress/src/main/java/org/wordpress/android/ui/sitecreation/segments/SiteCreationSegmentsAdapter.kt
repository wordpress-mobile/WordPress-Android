package org.wordpress.android.ui.sitecreation.segments

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.SegmentUiState
import org.wordpress.android.ui.sitecreation.segments.SiteCreationSegmentViewHolder.SegmentsHeaderViewHolder
import org.wordpress.android.ui.sitecreation.segments.SiteCreationSegmentViewHolder.SegmentsItemViewHolder
import org.wordpress.android.ui.sitecreation.segments.SiteCreationSegmentViewHolder.SegmentsProgressViewHolder
import org.wordpress.android.util.image.ImageManager

private const val headerViewType: Int = 1
private const val progressViewType: Int = 2
private const val segmentViewType: Int = 3

class SiteCreationSegmentsAdapter(
    private val imageManager: ImageManager
) : Adapter<SiteCreationSegmentViewHolder>() {
    private val items = mutableListOf<SegmentsItemUiState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteCreationSegmentViewHolder {
        return when (viewType) {
            headerViewType -> SegmentsHeaderViewHolder(parent)
            progressViewType -> SegmentsProgressViewHolder(parent)
            segmentViewType -> SegmentsItemViewHolder(parent, imageManager)
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SiteCreationSegmentViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(newItems: List<SegmentsItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(SegmentsDiffUtils(items.toList(), newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HeaderUiState -> headerViewType
            is ProgressUiState -> progressViewType
            is SegmentUiState -> segmentViewType
        }
    }

    private class SegmentsDiffUtils(
        val oldItems: List<SegmentsItemUiState>,
        val newItems: List<SegmentsItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is HeaderUiState -> true // it's an object
                is ProgressUiState -> true // it's an object
                is SegmentUiState -> oldItem.segmentId == (newItem as SegmentUiState).segmentId
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
