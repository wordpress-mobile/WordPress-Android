package org.wordpress.android.ui.mysite.cards.quicklinksribbon

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import org.wordpress.android.databinding.QuickLinkRibbonListBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

private const val Y_BUFFER = 10

class QuickLinkRibbonViewHolder(
    parent: ViewGroup
) : MySiteCardAndItemViewHolder<QuickLinkRibbonListBinding>(
        parent.viewBinding(QuickLinkRibbonListBinding::inflate)
) {
    init {
        with(binding.quickLinkRibbonItemList) {
            if (adapter == null) {
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                adapter = QuickLinkRibbonItemAdapter()
            }
        }
    }

    fun bind(quickLinkRibbon: QuickLinkRibbon) = with(binding) {
        setOnTouchItemListener()
        (quickLinkRibbonItemList.adapter as QuickLinkRibbonItemAdapter).update(quickLinkRibbon.quickLinkRibbonItems)
        if (quickLinkRibbon.showStatsFocusPoint) {
            quickLinkRibbonItemList.smoothScrollToPosition(0)
        }
        if (quickLinkRibbon.showPagesFocusPoint) {
            quickLinkRibbonItemList.smoothScrollToPosition(2)
        }
        if (quickLinkRibbon.showMediaFocusPoint) {
            quickLinkRibbonItemList.smoothScrollToPosition(quickLinkRibbon.quickLinkRibbonItems.size)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchItemListener() = with(binding) {
        val gestureDetector = GestureDetector(quickLinkRibbonItemList.context, GestureListener())
        quickLinkRibbonItemList.addOnItemTouchListener(object : OnItemTouchListener {
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

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        /**
         * Capture the DOWN as soon as it's detected to prevent the viewPager from intercepting touch events
         * We need to do this immediately, because if we don't, then the next move event could potentially
         * trigger the viewPager to switch tabs
         */
        override fun onDown(e: MotionEvent?): Boolean = with(binding) {
            quickLinkRibbonItemList.parent.requestDisallowInterceptTouchEvent(true)
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
                quickLinkRibbonItemList.parent.requestDisallowInterceptTouchEvent(true)
            } else if (kotlin.math.abs(distanceY) > Y_BUFFER) {
                // Detected a vertical scroll allow the viewpager to switch tabs
                quickLinkRibbonItemList.parent.requestDisallowInterceptTouchEvent(false)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }
}
