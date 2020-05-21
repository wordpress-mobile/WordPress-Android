package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.stats_block_single_activity.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.util.ContentDescriptionListAnnouncer

private const val SIZE_PADDING = 32
private const val GAP = 8
private const val BLOCK_WIDTH = 104
private const val SPAN_COUNT = 7

class ActivityViewHolder(val parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_activity_item
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val firstBlock = itemView.findViewById<LinearLayout>(R.id.first_activity)
    private val secondBlock = itemView.findViewById<LinearLayout>(R.id.second_activity)
    private val thirdBlock = itemView.findViewById<LinearLayout>(R.id.third_activity)

    fun bind(
        item: ActivityItem
    ) {
        drawBlock(firstBlock.activity, item.blocks[0].boxes)
        firstBlock.label.text = item.blocks[0].label
        if (item.blocks.size > 1) {
            drawBlock(secondBlock.activity, item.blocks[1].boxes)
            secondBlock.label.text = item.blocks[1].label
        }
        if (item.blocks.size > 2) {
            drawBlock(thirdBlock.activity, item.blocks[2].boxes)
            thirdBlock.label.text = item.blocks[2].label
        }
        val widthInDp = parent.width / parent.context.resources.displayMetrics.density
        if (widthInDp > BLOCK_WIDTH + 2 * GAP) {
            updateVisibility(item, widthInDp)
        } else {
            coroutineScope.launch {
                delay(50)
                updateVisibility(item, parent.width / parent.context.resources.displayMetrics.density)
            }
        }

        setupBlocksForAccessibility(item)
    }

    private fun setupBlocksForAccessibility(item: ActivityItem) {
        val blocks = listOf(firstBlock, secondBlock, thirdBlock)

        blocks.forEachIndexed { index, block ->
            block.label.contentDescription = item.blocks[index].contentDescription

            val announcer = ContentDescriptionListAnnouncer()
            announcer.setupAnnouncer(
                    R.string.stats_posting_activity_empty_description,
                    R.string.stats_posting_activity_end_description,
                    R.string.stats_posting_activity_action,
                    requireNotNull(item.blocks[index].activityContentDescriptions), block
            )
        }
    }

    private fun updateVisibility(
        item: ActivityItem,
        widthInDp: Float
    ) {
        val canFitTwoBlocks = widthInDp > 2 * BLOCK_WIDTH + GAP + SIZE_PADDING
        if (canFitTwoBlocks && item.blocks.size > 1) {
            secondBlock.visibility = View.VISIBLE
        } else {
            secondBlock.visibility = View.GONE
        }
        val canFitThreeBlocks = widthInDp > 3 * BLOCK_WIDTH + 2 * GAP + SIZE_PADDING
        if (canFitThreeBlocks && item.blocks.size > 2) {
            firstBlock.visibility = View.VISIBLE
        } else {
            firstBlock.visibility = View.GONE
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
        if (recyclerView.itemDecorationCount == 0) {
            val offsets = recyclerView.resources.getDimensionPixelSize(R.dimen.stats_activity_spacing)
            recyclerView.addItemDecoration(
                    object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(
                            outRect: Rect,
                            view: View,
                            parent: RecyclerView,
                            state: RecyclerView.State
                        ) {
                            super.getItemOffsets(outRect, view, parent, state)
                            outRect.set(offsets, offsets, offsets, offsets)
                        }
                    }
            )
        }
        (recyclerView.adapter as MonthActivityAdapter).update(boxes)
    }
}
