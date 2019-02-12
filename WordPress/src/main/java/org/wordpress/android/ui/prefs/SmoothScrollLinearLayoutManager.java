package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * LinearLayoutManager with smooth scrolling and custom duration (in milliseconds).
 */
public class SmoothScrollLinearLayoutManager extends LinearLayoutManager {
    private final int mDuration;

    public SmoothScrollLinearLayoutManager(Context context, int orientation, boolean reverseLayout, int duration) {
        super(context, orientation, reverseLayout);
        this.mDuration = duration;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        final View firstVisibleChild = recyclerView.getChildAt(0);
        final int itemHeight = firstVisibleChild.getHeight();
        final int currentPosition = recyclerView.getChildAdapterPosition(firstVisibleChild);
        int distanceInPixels = Math.abs((currentPosition - position) * itemHeight);

        if (distanceInPixels == 0) {
            distanceInPixels = (int) Math.abs(firstVisibleChild.getY());
        }

        final SmoothScroller smoothScroller =
                new SmoothScroller(recyclerView.getContext(), distanceInPixels, mDuration);
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    private class SmoothScroller extends LinearSmoothScroller {
        private final float mDistanceInPixels;
        private final float mDuration;

        SmoothScroller(Context context, int distanceInPixels, int duration) {
            super(context);
            this.mDistanceInPixels = distanceInPixels;
            this.mDuration = duration;
        }

        @Override
        protected int calculateTimeForScrolling(int distance) {
            final float proportion = (float) distance / mDistanceInPixels;
            return (int) (mDuration * proportion);
        }

        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return SmoothScrollLinearLayoutManager.this.computeScrollVectorForPosition(targetPosition);
        }
    }
}
