package org.wordpress.android.ui.reader.discover.viewholders

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import org.wordpress.android.databinding.ReaderInterestCardBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderInterestAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

private const val Y_BUFFER = 10

class ReaderInterestsCardViewHolder(
    uiHelpers: UiHelpers,
    parentView: ViewGroup
) : ReaderViewHolder<ReaderInterestCardBinding>(parentView.viewBinding(ReaderInterestCardBinding::inflate)) {
    init {
        with(binding.interestsList) {
            if (adapter == null) {
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                val readerInterestAdapter = ReaderInterestAdapter(uiHelpers)
                adapter = readerInterestAdapter
            }
        }
    }

    override fun onBind(uiState: ReaderCardUiState) = with(binding) {
        uiState as ReaderInterestsCardUiState
        setOnTouchItemListener()
        (interestsList.adapter as ReaderInterestAdapter).update(uiState.interest)
    }

    private fun setOnTouchItemListener() = with(binding) {
        val gestureDetector = GestureDetector(interestsList.context, GestureListener())
        interestsList.addOnItemTouchListener(object : OnItemTouchListener {
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
            interestsList.parent.requestDisallowInterceptTouchEvent(true)
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
                interestsList.parent.requestDisallowInterceptTouchEvent(true)
            } else if (kotlin.math.abs(distanceY) > Y_BUFFER) {
                // Detected a vertical scroll allow the viewpager to switch tabs
                interestsList.parent.requestDisallowInterceptTouchEvent(false)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }
}
