package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;


import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.scrollIntoView;

public class ReaderPage {
    public ReaderPage go() {
        clickOn(R.id.nav_reader);

        return this;
    }

    public ReaderPage tapFollowingTab() {
        clickOn(onView(withText("FOLLOWING")));

        return this;
    }

    public ReaderViewPage openPost(String postTitle) {
        ViewInteraction post = onView(withChild(withText(postTitle)));

        scrollIntoView(R.id.reader_recycler_view, post, (float) 1);
        clickOn(postTitle);

        return new ReaderViewPage().waitUntilLoaded();
    }
}
