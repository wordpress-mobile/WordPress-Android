package org.wordpress.android.ui.mysite

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.quick_start_card.view.*
import kotlinx.android.synthetic.main.quick_start_task_card.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.QuickStartTaskCardAdapter.QuickStartTaskCardViewHolder
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard.QuickStartTaskCard
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.RecyclerItemDecoration

class QuickStartCardViewHolder(
    parent: ViewGroup,
    private val viewPool: RecycledViewPool,
    private val nestedScrollStates: Bundle
) : MySiteItemViewHolder(parent, R.layout.quick_start_card) {
    private var currentItem: QuickStartCard? = null
    private val lowEmphasisAlpha = ResourcesCompat.getFloat(itemView.resources, R.dimen.emphasis_low)

    init {
        itemView.apply {
            quick_start_card_more_button.let { TooltipCompat.setTooltipText(it, it.contentDescription) }
            quick_start_card_recycler_view.apply {
                adapter = QuickStartTaskCardAdapter()
                layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
                setRecycledViewPool(viewPool)
                addItemDecoration(RecyclerItemDecoration(DisplayUtils.dpToPx(context, 10), 0))
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            currentItem?.let { saveScrollState(recyclerView, it.id) }
                        }
                    }
                })
            }
        }
    }

    fun bind(item: QuickStartCard) = itemView.apply {
        currentItem = item

        ObjectAnimator.ofInt(quick_start_card_progress, "progress", item.progress).setDuration(600).start()

        val progressIndicatorColor = ContextCompat.getColor(context, item.accentColor)
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
        (quick_start_card_recycler_view.adapter as? QuickStartTaskCardAdapter)?.loadData(item.taskCards)
        restoreScrollState(quick_start_card_recycler_view, item.id)
        quick_start_card_more_button.setOnClickListener { item.onMoreClick?.click() }
    }

    fun onRecycled() {
        currentItem?.let { saveScrollState(itemView.quick_start_card_recycler_view, it.id) }
        currentItem = null
    }

    private fun saveScrollState(recyclerView: RecyclerView, key: String) {
        recyclerView.layoutManager?.onSaveInstanceState()?.let { nestedScrollStates.putParcelable(key, it) }
    }

    private fun restoreScrollState(recyclerView: RecyclerView, key: String) {
        recyclerView.layoutManager?.apply {
            val scrollState = nestedScrollStates.getParcelable<Parcelable>(key)
            if (scrollState != null) {
                onRestoreInstanceState(scrollState)
            } else {
                scrollToPosition(0)
            }
        }
    }
}

class QuickStartTaskCardAdapter : Adapter<QuickStartTaskCardViewHolder>() {
    private var items = listOf<QuickStartTaskCard>()

    fun loadData(newItems: List<QuickStartTaskCard>) {
        val diffResult = DiffUtil.calculateDiff(QuickStartTaskCardAdapterDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QuickStartTaskCardViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.quick_start_task_card, parent, false)
    )

    override fun onBindViewHolder(holder: QuickStartTaskCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class QuickStartTaskCardViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(taskCard: QuickStartTaskCard) = itemView.apply {
            dummy_task_title.text = taskCard.title
            dummy_task_description.text = taskCard.description

            val alpha = if (taskCard.done) 0.2f else 1.0f
            dummy_task_title.alpha = alpha
            dummy_task_description.alpha = alpha
            dummy_task_background.alpha = alpha
        }
    }

    inner class QuickStartTaskCardAdapterDiffCallback(
        private val oldItems: List<QuickStartTaskCard>,
        private val newItems: List<QuickStartTaskCard>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition].id == newItems[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
