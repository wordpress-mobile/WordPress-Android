package org.wordpress.android.ui.mysite.dynamiccards.quickstart

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import org.wordpress.android.R
import org.wordpress.android.databinding.QuickStartDynamicCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.viewBinding

class QuickStartDynamicCardViewHolder(
    parent: ViewGroup,
    private val viewPool: RecycledViewPool,
    private val nestedScrollStates: Bundle,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<QuickStartDynamicCardBinding>(
        parent.viewBinding(QuickStartDynamicCardBinding::inflate)
) {
    private var currentItem: QuickStartDynamicCard? = null
    private val lowEmphasisAlpha = ResourcesCompat.getFloat(itemView.resources, R.dimen.emphasis_low)

    init {
        with(binding) {
            quickStartCardMoreButton.let { TooltipCompat.setTooltipText(it, it.contentDescription) }
            quickStartCardRecyclerView.apply {
                adapter = QuickStartTaskCardAdapter(uiHelpers)
                layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
                setRecycledViewPool(viewPool)
                addItemDecoration(
                        QuickStartListItemDecoration(
                                resources.getDimensionPixelSize(R.dimen.margin_extra_small),
                                resources.getDimensionPixelSize(R.dimen.margin_large)
                        )
                )
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            currentItem?.let { saveScrollState(recyclerView, it.id.toString()) }
                        }
                    }
                })
            }
        }
    }

    fun bind(item: QuickStartDynamicCard) = with(binding) {
        currentItem = item

        ObjectAnimator.ofInt(quickStartCardProgress, PROGRESS_PROPERTY_NAME, item.progress)
                .setDuration(PROGRESS_DURATION).start()

        val progressIndicatorColor = ContextCompat.getColor(root.context, item.accentColor)
        val progressTrackColor = ColorUtils.applyEmphasisToColor(progressIndicatorColor, lowEmphasisAlpha)
        quickStartCardProgress.progressBackgroundTintList = ColorStateList.valueOf(progressTrackColor)
        quickStartCardProgress.progressTintList = ColorStateList.valueOf(progressIndicatorColor)

        quickStartCardTitle.text = uiHelpers.getTextOfUiString(root.context, item.title)
        (quickStartCardRecyclerView.adapter as? QuickStartTaskCardAdapter)?.loadData(item.taskCards)
        restoreScrollState(quickStartCardRecyclerView, item.id.toString())
        quickStartCardMoreButton.setOnClickListener { item.onMoreClick.click() }
    }

    fun onRecycled() {
        currentItem?.let { saveScrollState(binding.quickStartCardRecyclerView, it.id.toString()) }
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

    companion object {
        private const val PROGRESS_PROPERTY_NAME = "progress"
        private const val PROGRESS_DURATION = 600L
    }
}
