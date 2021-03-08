package org.wordpress.android.ui.reader.discover.viewholders

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import kotlinx.android.synthetic.main.reader_interest_card.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderInterestAdapter
import org.wordpress.android.ui.utils.UiHelpers

private const val Y_BUFFER = 10

class ReaderInterestsCardViewHolder(
    uiHelpers: UiHelpers,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_interest_card) {
    init {
        if (interests_list.adapter == null) {
            interests_list.layoutManager = LinearLayoutManager(interests_list.context, RecyclerView.HORIZONTAL, false)
            val readerInterestAdapter = ReaderInterestAdapter(uiHelpers)
            interests_list.adapter = readerInterestAdapter
        }
    }

    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderInterestsCardUiState
        setOnTouchItemListener()
        (interests_list.adapter as ReaderInterestAdapter).update(uiState.interest)
    }

    private fun setOnTouchItemListener() {
        val gestureDetector = GestureDetector(interests_list.context, GestureListener())
        interests_list.addOnItemTouchListener(object : OnItemTouchListener {
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
        override fun onDown(e: MotionEvent?): Boolean {
            interests_list.parent.requestDisallowInterceptTouchEvent(true)
            return super.onDown(e)
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (kotlin.math.abs(distanceX) > kotlin.math.abs(distanceY)) {
                // Detected a horizontal scroll, prevent the viewpager from switching tabs
                interests_list.parent.requestDisallowInterceptTouchEvent(true)
            } else if (kotlin.math.abs(distanceY) > Y_BUFFER) {
                // Detected a vertical scroll allow the viewpager to switch tabs
                interests_list.parent.requestDisallowInterceptTouchEvent(false)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }
}
