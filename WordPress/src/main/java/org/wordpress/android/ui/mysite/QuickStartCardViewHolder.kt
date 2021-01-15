package org.wordpress.android.ui.mysite

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import kotlinx.android.synthetic.main.quick_start_card.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.RecyclerItemDecoration

class QuickStartCardViewHolder(
    parent: ViewGroup,
    private val viewPool: RecycledViewPool,
    private val nestedScrollStates: Bundle,
    private val uiHelpers: UiHelpers
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

        quick_start_card_title.text = uiHelpers.getTextOfUiString(context, item.title)
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
