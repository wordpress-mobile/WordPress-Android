package org.wordpress.android.ui.reader.discover.viewholders

import android.graphics.Rect
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderInterestCardBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderInterestAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.widgets.RecyclerItemDecoration

private const val Y_BUFFER = 10

class ReaderInterestsCardViewHolder(
    uiHelpers: UiHelpers,
    parentView: ViewGroup
) : ReaderViewHolder<ReaderInterestCardBinding>(parentView.viewBinding(ReaderInterestCardBinding::inflate)) {
    init {
        with(binding.recommendedTags) {
            if (adapter == null) {
                layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP)
                val readerInterestAdapter = ReaderInterestAdapter(uiHelpers)
                setItemSpacing()
                adapter = readerInterestAdapter
            }
        }
    }

    private fun RecyclerView.setItemSpacing() {
        val spacingHorizontal = resources.getDimensionPixelSize(R.dimen.margin_small)
        addItemDecoration(RecyclerItemDecoration(spacingHorizontal, 0, false))
        addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) {
                val spacingVertical = resources.getDimensionPixelSize(R.dimen.margin_medium)
                outRect.set(0, 0, 0, spacingVertical)
            }
        })
    }

    override fun onBind(uiState: ReaderCardUiState) = with(binding) {
        uiState as ReaderCardUiState.ReaderInterestsCardUiState
        setOnTouchItemListener()
        (recommendedTags.adapter as ReaderInterestAdapter).update(uiState.interest)
    }

    private fun setOnTouchItemListener() = with(binding) {
        val gestureDetector = GestureDetector(recommendedTags.context, GestureListener())
        recommendedTags.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
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
        override fun onDown(e: MotionEvent): Boolean = with(binding) {
            recommendedTags.parent.requestDisallowInterceptTouchEvent(true)
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean = with(binding) {
            if (kotlin.math.abs(distanceX) > kotlin.math.abs(distanceY)) {
                // Detected a horizontal scroll, prevent the viewpager from switching tabs
                recommendedTags.parent.requestDisallowInterceptTouchEvent(true)
            } else if (kotlin.math.abs(distanceY) > Y_BUFFER) {
                // Detected a vertical scroll allow the viewpager to switch tabs
                recommendedTags.parent.requestDisallowInterceptTouchEvent(false)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }
}
