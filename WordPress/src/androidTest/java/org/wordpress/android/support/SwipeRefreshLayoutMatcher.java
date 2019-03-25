package org.wordpress.android.support;

import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class SwipeRefreshLayoutMatcher extends TypeSafeMatcher<View> {
    private final boolean matchesRefreshing;

    public SwipeRefreshLayoutMatcher(boolean matchesRefreshing) {
        super(View.class);
        this.matchesRefreshing = matchesRefreshing;
    }

    @Override
    protected boolean matchesSafely(View item) {
        if (item instanceof SwipeRefreshLayout) {
            SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) item;
            return refreshLayout.isRefreshing() == matchesRefreshing;
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is a swipe refresh layout");
    }
}
