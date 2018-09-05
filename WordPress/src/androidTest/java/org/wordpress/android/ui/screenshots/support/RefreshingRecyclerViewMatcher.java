package org.wordpress.android.ui.screenshots.support;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.wordpress.android.ui.FilteredRecyclerView;

public class RefreshingRecyclerViewMatcher extends TypeSafeMatcher<View> {
    public RefreshingRecyclerViewMatcher() {
        super(View.class);
    }

    @Override
    protected boolean matchesSafely(View item) {
        if (item instanceof FilteredRecyclerView) {
            FilteredRecyclerView recyclerView = (FilteredRecyclerView) item;
            return recyclerView.isRefreshing();
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is a refreshing recycler view");
    }
}
