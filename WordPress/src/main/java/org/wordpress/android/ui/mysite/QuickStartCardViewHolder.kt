package org.wordpress.android.ui.mysite

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.quick_start_card.view.*
import kotlinx.android.synthetic.main.quick_start_task_card.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.DummyTaskAdapter.DummyTaskViewHolder
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard.DummyTask
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.RecyclerItemDecoration

class QuickStartCardViewHolder(
    parent: ViewGroup,
    private val viewPool: RecycledViewPool
) : MySiteItemViewHolder(parent, R.layout.quick_start_card) {
    private val lowEmphasisAlpha = ResourcesCompat.getFloat(itemView.resources, R.dimen.emphasis_low)

    init {
        itemView.apply {
            quick_start_card_more_button.let { TooltipCompat.setTooltipText(it, it.contentDescription) }
            quick_start_card_recycler_view.apply {
                adapter = DummyTaskAdapter()
                layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
                setRecycledViewPool(viewPool)
                addItemDecoration(RecyclerItemDecoration(DisplayUtils.dpToPx(context, 8), 0))
            }
        }
    }

    fun bind(item: QuickStartCard) = itemView.apply {
        ObjectAnimator.ofInt(quick_start_card_progress, "progress", item.progress).setDuration(600).start()

        val progressIndicatorColor = ContextCompat.getColor(context, item.progressColor.indicatorColor)
        val progressTrackColor = ColorUtils.applyEmphasisToColor(progressIndicatorColor, lowEmphasisAlpha)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            quick_start_card_progress.progressBackgroundTintList = ColorStateList.valueOf(progressTrackColor)
            quick_start_card_progress.progressTintList = ColorStateList.valueOf(progressIndicatorColor)
        } else {
            // Workaround for Lollipop
            val progressDrawable = quick_start_card_progress.progressDrawable.mutate() as LayerDrawable
            val backgroundLayer = progressDrawable.findDrawableByLayerId(android.R.id.background)
            val progressLayer = progressDrawable.findDrawableByLayerId(android.R.id.progress)
            backgroundLayer.colorFilter = createBlendModeColorFilterCompat(progressTrackColor, SRC_IN)
            progressLayer.colorFilter = createBlendModeColorFilterCompat(progressIndicatorColor, SRC_IN)
            quick_start_card_progress.progressDrawable = progressDrawable
        }

        quick_start_card_title.text = item.title
        (quick_start_card_recycler_view.adapter as? DummyTaskAdapter)?.loadData(item.tasks)
        quick_start_card_more_button.setOnClickListener { item.onMoreClick?.click() }
    }
}

class DummyTaskAdapter : Adapter<DummyTaskViewHolder>() {
    private var items = listOf<DummyTask>()

    fun loadData(newItems: List<DummyTask>) {
        val diffResult = DiffUtil.calculateDiff(DummyTaskAdapterDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DummyTaskViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.quick_start_task_card, parent, false)
    )

    override fun onBindViewHolder(holder: DummyTaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class DummyTaskViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(task: DummyTask) = itemView.apply {
            dummy_task_title.text = task.title
            dummy_task_description.text = task.description

            val alpha = if (task.done) 0.2f else 1.0f
            dummy_task_title.alpha = alpha
            dummy_task_description.alpha = alpha
            dummy_task_background.alpha = alpha
        }
    }

    inner class DummyTaskAdapterDiffCallback(
        private val oldItems: List<DummyTask>,
        private val newItems: List<DummyTask>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition].id == newItems[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
