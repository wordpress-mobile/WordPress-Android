package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.databinding.StatsBlockActivityItemBinding
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.util.ContentDescriptionListAnnouncer
import org.wordpress.android.util.extensions.viewBinding

private const val SIZE_PADDING = 32
private const val GAP = 8
private const val BLOCK_WIDTH = 104
private const val SPAN_COUNT = 7

class ActivityViewHolder(
    parent: ViewGroup,
    val binding: StatsBlockActivityItemBinding = parent.viewBinding(StatsBlockActivityItemBinding::inflate)
) : BlockListItemViewHolder(
        binding.root
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun bind(
        item: ActivityItem
    ) = with(binding) {
        drawBlock(firstActivity.activity, item.blocks[0].boxes)
        firstActivity.label.text = item.blocks[0].label
        if (item.blocks.size > 1) {
            drawBlock(secondActivity.activity, item.blocks[1].boxes)
            secondActivity.label.text = item.blocks[1].label
        }
        if (item.blocks.size > 2) {
            drawBlock(thirdActivity.activity, item.blocks[2].boxes)
            thirdActivity.label.text = item.blocks[2].label
        }
        val widthInDp = root.width / root.context.resources.displayMetrics.density
        if (widthInDp > BLOCK_WIDTH + 2 * GAP) {
            updateVisibility(item, widthInDp)
        } else {
            coroutineScope.launch {
                delay(50)
                updateVisibility(item, root.width / root.context.resources.displayMetrics.density)
            }
        }

        setupBlocksForAccessibility(item)
    }

    private fun StatsBlockActivityItemBinding.setupBlocksForAccessibility(item: ActivityItem) {
        val blocks = listOf(firstActivity, secondActivity, thirdActivity)

        blocks.forEachIndexed { index, block ->
            block.label.contentDescription = item.blocks[index].contentDescription

            val announcer = ContentDescriptionListAnnouncer()
            announcer.setupAnnouncer(
                    R.string.stats_posting_activity_empty_description,
                    R.string.stats_posting_activity_end_description,
                    R.string.stats_posting_activity_action,
                    requireNotNull(item.blocks[index].activityContentDescriptions), block.root
            )
        }
    }

    private fun StatsBlockActivityItemBinding.updateVisibility(
        item: ActivityItem,
        widthInDp: Float
    ) {
        val canFitTwoBlocks = widthInDp > 2 * BLOCK_WIDTH + GAP + SIZE_PADDING
        if (canFitTwoBlocks && item.blocks.size > 1) {
            secondActivity.root.visibility = View.VISIBLE
        } else {
            secondActivity.root.visibility = View.GONE
        }
        val canFitThreeBlocks = widthInDp > 3 * BLOCK_WIDTH + 2 * GAP + SIZE_PADDING
        if (canFitThreeBlocks && item.blocks.size > 2) {
            firstActivity.root.visibility = View.VISIBLE
        } else {
            firstActivity.root.visibility = View.GONE
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
