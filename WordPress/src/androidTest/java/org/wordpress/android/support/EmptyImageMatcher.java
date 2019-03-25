package org.wordpress.android.support;

import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class EmptyImageMatcher extends TypeSafeMatcher<View> {
    public EmptyImageMatcher() {
        super(View.class);
    }

    @Override
    protected boolean matchesSafely(View item) {
        if (item instanceof ImageView) {
            ImageView imageView = (ImageView) item;
            return imageView.getDrawable() == null;
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is an unloaded image");
    }
}
