package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;


import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.isElementCompletelyDisplayed;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.swipeUpOnView;

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

        scrollIntoView(post);
        clickOn(postTitle);

        return new ReaderViewPage().waitUntilLoaded();
    }

    private void scrollIntoView(ViewInteraction postContainer) {
        int swipeCount = 0;
        while (!isElementCompletelyDisplayed(postContainer) && swipeCount < 10) {
            swipeUpOnView(R.id.reader_recycler_view, (float) 1);
            swipeCount += 1;
        }
    }

    public void dismissReaderViewIfNeeded() {
        if (isElementDisplayed(onView(withId(R.id.scroll_view_reader)))) {
            pressBack();
        }
    }
}
