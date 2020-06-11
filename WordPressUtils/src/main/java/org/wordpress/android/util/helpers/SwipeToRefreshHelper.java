package org.wordpress.android.util.helpers;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

public class SwipeToRefreshHelper implements OnRefreshListener {
    private CustomSwipeRefreshLayout mSwipeRefreshLayout;
    private RefreshListener mRefreshListener;
    private boolean mRefreshing;

    public interface RefreshListener {
        void onRefreshStarted();
    }

    /**
     * Helps {@link org.wordpress.android.util.widgets.CustomSwipeRefreshLayout} by passing the
     * {@link SwipeRefreshLayout}, {@link RefreshListener}, and color.
     *
     * @param context {@link Context} in which this layout is used.
     * @param swipeRefreshLayout {@link CustomSwipeRefreshLayout} for refreshing the contents
     * of a view via a vertical swipe gesture.
     * @param listener {@link RefreshListener} notified when a refresh is triggered
     * via the swipe gesture.
     *
     * @deprecated Use {@link #SwipeToRefreshHelper(CustomSwipeRefreshLayout, RefreshListener, int, int...)} instead.
     */
    @Deprecated
    public SwipeToRefreshHelper(Context context, CustomSwipeRefreshLayout swipeRefreshLayout,
                                RefreshListener listener) {
        init(swipeRefreshLayout, listener, ContextCompat.getColor(context, android.R.color.white),
                android.R.color.holo_blue_dark);
    }

    /**
     * Helps {@link org.wordpress.android.util.widgets.CustomSwipeRefreshLayout} by passing the
     * {@link SwipeRefreshLayout}, {@link RefreshListener}, and color(s).
     *
     * @param swipeRefreshLayout {@link CustomSwipeRefreshLayout} for refreshing the contents
     * of a view via a vertical swipe gesture.
     * @param listener {@link RefreshListener} notified when a refresh is triggered
     * via the swipe gesture.
     * @param progressAnimationColors Comma-separated color resource integers used in the progress
     * animation. The first color will also be the color of the bar
     * that grows in response to a user swipe gesture.
     */
    public SwipeToRefreshHelper(CustomSwipeRefreshLayout swipeRefreshLayout, RefreshListener listener,
                                @ColorInt int backgroundColor,
                                @ColorRes int... progressAnimationColors) {
        init(swipeRefreshLayout, listener, backgroundColor, progressAnimationColors);
    }

    /**
     * Initializes {@link org.wordpress.android.util.widgets.CustomSwipeRefreshLayout} by assigning
     * {@link SwipeRefreshLayout}, {@link RefreshListener}, and color(s).
     *
     * @param swipeRefreshLayout {@link CustomSwipeRefreshLayout} for refreshing the contents
     * of a view via a vertical swipe gesture.
     * @param listener {@link RefreshListener} notified when a refresh is triggered
     * via the swipe gesture.
     * @param progressAnimationColors Comma-separated color resource integers used in the progress
     * animation. The first color will also be the color of the bar
     * that grows in response to a user swipe gesture.
     */
    public void init(CustomSwipeRefreshLayout swipeRefreshLayout, RefreshListener listener,
                     @ColorInt int backgroundColor,
                     @ColorRes int... progressAnimationColors) {
        mRefreshListener = listener;
        mSwipeRefreshLayout = swipeRefreshLayout;
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(backgroundColor);
        mSwipeRefreshLayout.setColorSchemeResources(progressAnimationColors);
    }

    public void setRefreshing(boolean refreshing) {
        mRefreshing = refreshing;
        // Delayed refresh, it fixes https://code.google.com/p/android/issues/detail?id=77712
        // 50ms seems a good compromise (always worked during tests) and fast enough so user can't notice the delay
        if (refreshing) {
            mSwipeRefreshLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // use mRefreshing so if the refresh takes less than 50ms, loading indicator won't show up.
                    mSwipeRefreshLayout.setRefreshing(mRefreshing);
                }
            }, 50);
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout.isRefreshing();
    }

    @Override
    public void onRefresh() {
        mRefreshListener.onRefreshStarted();
    }

    public void setEnabled(boolean enabled) {
        mSwipeRefreshLayout.setEnabled(enabled);
    }
}
