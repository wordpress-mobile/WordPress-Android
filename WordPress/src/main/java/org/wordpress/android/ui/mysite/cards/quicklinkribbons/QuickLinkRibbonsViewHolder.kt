package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import org.wordpress.android.databinding.QuickLinkRibbonsListBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbons
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

private const val Y_BUFFER = 100

class QuickLinkRibbonsViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<QuickLinkRibbonsListBinding>(
        parent.viewBinding(QuickLinkRibbonsListBinding::inflate)
) {
    fun bind(quickLinkRibbons: QuickLinkRibbons) = with(binding) {
        setOnTouchItemListener()
        pages.setOnClickListener { quickLinkRibbons.onPagesClick.click() }
        posts.setOnClickListener { quickLinkRibbons.onPostsClick.click() }
        media.setOnClickListener { quickLinkRibbons.onMediaClick.click() }
        stats.setOnClickListener { quickLinkRibbons.onStatsClick.click() }
        uiHelpers.updateVisibility(pages, quickLinkRibbons.showPages)
    }

    // we are not detecting click events in the scroll view so we can ignore this
    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchItemListener() = with(binding) {
        val gestureDetector = GestureDetector(quickLinkScrollView.context, GestureListener())
        quickLinkScrollView.setOnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        /**
         * Capture the DOWN as soon as it's detected to prevent the viewPager from intercepting touch events
         * We need to do this immediately, because if we don't, then the next move event could potentially
         * trigger the viewPager to switch tabs
         */
        override fun onDown(e: MotionEvent?): Boolean = with(binding) {
            quickLinkScrollView.parent.requestDisallowInterceptTouchEvent(true)
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
                quickLinkScrollView.parent.requestDisallowInterceptTouchEvent(true)
            } else if (kotlin.math.abs(distanceY) > Y_BUFFER) {
                // Detected a vertical scroll allow the viewpager to switch tabs
                quickLinkScrollView.parent.requestDisallowInterceptTouchEvent(false)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }
}
