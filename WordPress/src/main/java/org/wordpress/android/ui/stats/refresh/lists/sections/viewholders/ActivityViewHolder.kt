package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.graphics.Rect
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.stats_block_single_activity.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box

private const val SIZE_PADDING = 32
private const val GAP = 8
private const val BLOCK_WIDTH = 104
private const val SPAN_COUNT = 7

class ActivityViewHolder(val parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_activity_item
) {
    private val firstBlock = itemView.findViewById<LinearLayout>(R.id.first_activity)
    private val secondBlock = itemView.findViewById<LinearLayout>(R.id.second_activity)
    private val thirdBlock = itemView.findViewById<LinearLayout>(R.id.third_activity)

    fun bind(
        item: ActivityItem
    ) {
        val widthInDp = parent.width / parent.context.resources.displayMetrics.density
        drawBlock(firstBlock.activity, item.blocks[0].boxes)
        firstBlock.label.text = item.blocks[0].label
        val canFitTwoBlocks = widthInDp > 2 * BLOCK_WIDTH + GAP + SIZE_PADDING
        if (canFitTwoBlocks && item.blocks.size > 1) {
            secondBlock.visibility = View.VISIBLE
            drawBlock(secondBlock.activity, item.blocks[1].boxes)
            secondBlock.label.text = item.blocks[1].label
        } else {
            secondBlock.visibility = View.GONE
        }
        val canFitThreeBlocks = widthInDp > 3 * BLOCK_WIDTH + 2 * GAP + SIZE_PADDING
        if (canFitThreeBlocks && item.blocks.size > 2) {
            thirdBlock.visibility = View.VISIBLE
            drawBlock(thirdBlock.activity, item.blocks[2].boxes)
            thirdBlock.label.text = item.blocks[2].label
        } else {
            thirdBlock.visibility = View.GONE
        }
    }

    private fun drawBlock(recyclerView: RecyclerView, boxes: List<Box>) {
        if (recyclerView.adapter == null) {
            recyclerView.adapter = MonthActivityAdapter()
        }
        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = GridLayoutManager(
                    recyclerView.context,
                    SPAN_COUNT,
                    GridLayoutManager.HORIZONTAL,
                    false
            )
        }
        val offsets = recyclerView.resources.getDimensionPixelSize(R.dimen.stats_activity_spacing)
        recyclerView.addItemDecoration(
                object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                        super.getItemOffsets(outRect, view, parent, state)
                        outRect.set(offsets, offsets, offsets, offsets)
                    }
                }
        )
        (recyclerView.adapter as MonthActivityAdapter).update(boxes)
    }
}
