package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class PostPreviewPage {
    private ViewInteraction mPreview;

    public PostPreviewPage() {
        mPreview = onView(withId(R.id.preview_container));
        mPreview.check(matches(isDisplayed()));
    }
}
