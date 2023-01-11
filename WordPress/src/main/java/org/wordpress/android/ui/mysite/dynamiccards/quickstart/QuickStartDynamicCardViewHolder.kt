package org.wordpress.android.ui.mysite.dynamiccards.quickstart

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import org.wordpress.android.R
import org.wordpress.android.databinding.QuickStartDynamicCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.extensions.viewBinding

private const val Y_BUFFER = 10

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
        setOnTouchItemListener()
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

    private fun setOnTouchItemListener() = with(binding) {
        val gestureDetector = GestureDetector(quickStartCardRecyclerView.context, GestureListener())
        quickStartCardRecyclerView.addOnItemTouchListener(object : OnItemTouchListener {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, e: MotionEvent): Boolean {
                return gestureDetector.onTouchEvent(e)
            }

            override fun onTouchEvent(recyclerView: RecyclerView, e: MotionEvent) {
                // NO OP
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // NO OP
            }
        })
    }

    companion object {
        private const val PROGRESS_PROPERTY_NAME = "progress"
        private const val PROGRESS_DURATION = 600L
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        /**
         * Capture the DOWN as soon as it's detected to prevent the viewPager from intercepting touch events
         * We need to do this immediately, because if we don't, then the next move event could potentially
         * trigger the viewPager to switch tabs
         */
        override fun onDown(e: MotionEvent?): Boolean = with(binding) {
            quickStartCardRecyclerView.parent.requestDisallowInterceptTouchEvent(true)
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean = with(binding) {
            if (kotlin.math.abs(distanceX) > kotlin.math.abs(distanceY)) {
                // Detected a horizontal scroll, prevent the viewpager from switching tabs
                quickStartCardRecyclerView.parent.requestDisallowInterceptTouchEvent(true)
            } else if (kotlin.math.abs(distanceY) > Y_BUFFER) {
                // Detected a vertical scroll allow the viewpager to switch tabs
                quickStartCardRecyclerView.parent.requestDisallowInterceptTouchEvent(false)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }
}
