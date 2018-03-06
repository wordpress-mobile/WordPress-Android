package org.wordpress.android.util;

import org.wordpress.android.R;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

public class WPSwipeToRefreshHelper {
    /**
     * Builds a {@link org.wordpress.android.util.helpers.SwipeToRefreshHelper} and returns a new
     * instance with colors designated for the WordPress app.
     *
     * @param swipeRefreshLayout {@link CustomSwipeRefreshLayout} for refreshing the contents
     * of a view via a vertical swipe gesture.
     * @param listener {@link RefreshListener} notified when a refresh is triggered
     * via the swipe gesture.
     */
    public static SwipeToRefreshHelper buildSwipeToRefreshHelper(CustomSwipeRefreshLayout swipeRefreshLayout,
                                                                 RefreshListener listener) {
        return new SwipeToRefreshHelper(swipeRefreshLayout, listener, R.color.color_primary, R.color.color_accent);
    }
}
