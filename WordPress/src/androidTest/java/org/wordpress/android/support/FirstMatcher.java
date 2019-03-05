package org.wordpress.android.support;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FirstMatcher extends TypeSafeMatcher<View> {
    private boolean mHasMatched = false;

    public FirstMatcher() {
        super(View.class);
    }

    @Override
    protected boolean matchesSafely(View item) {
        if (mHasMatched) {
            return false;
        }

        mHasMatched = true;
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("first instance.");
    }
}
