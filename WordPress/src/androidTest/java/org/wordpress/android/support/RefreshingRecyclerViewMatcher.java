package org.wordpress.android.support;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.wordpress.android.ui.FilteredRecyclerView;

public class RefreshingRecyclerViewMatcher extends TypeSafeMatcher<View> {
    private final boolean matchesRefreshing;

    public RefreshingRecyclerViewMatcher(boolean matchesRefreshing) {
        super(View.class);
        this.matchesRefreshing = matchesRefreshing;
    }

    @Override
    protected boolean matchesSafely(View item) {
        if (item instanceof FilteredRecyclerView) {
            FilteredRecyclerView recyclerView = (FilteredRecyclerView) item;
            return recyclerView.isRefreshing() == matchesRefreshing;
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is a refreshing recycler view");
    }
}
