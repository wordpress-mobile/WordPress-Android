package org.wordpress.android.support;

import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.wordpress.android.util.image.ImageType;

public class PlaceholderImageMatcher extends TypeSafeMatcher<View> {
    private ImageType mImageType;


    public PlaceholderImageMatcher(ImageType imageType) {
        super(View.class);
        mImageType = imageType;
    }

    @Override
    protected boolean matchesSafely(View item) {
        if (item instanceof ImageView) {
            ImageView view = (ImageView) item;
            return new PlaceholderComparison(mImageType)
                    .matches(view);
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is a placeholder for " + mImageType.name());
    }
}
