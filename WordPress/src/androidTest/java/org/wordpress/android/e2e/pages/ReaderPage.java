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
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static junit.framework.TestCase.assertTrue;

public class ReaderPage {
    public ReaderPage go() {
        clickOn(R.id.nav_reader);

        return this;
    }

    public ReaderPage tapFollowingTab() {
        clickOn(onView(withText("FOLLOWING")));

        return this;
    }

    public ReaderPage openPost(String postTitle) {
        ViewInteraction post = onView(withChild(withText(postTitle)));

        scrollIntoView(post);
        clickOn(post);

        return this;
    }

    private void scrollIntoView(ViewInteraction postContainer) {
        while (!isElementCompletelyDisplayed(postContainer)) {
            swipeUpOnView(R.id.reader_recycler_view, (float) 1);
        }
    }

    public void dismissReaderViewIfNeeded() {
        if (isElementDisplayed(onView(withId(R.id.scroll_view_reader)))) {
            pressBack();
        }
    }

    public ReaderPage verifyPostDisplayed(String title, String text) {
        assertTrue("Post title was not displayed",
                waitForElementToBeDisplayedWithoutFailure(onView(withText(title))));
        assertTrue("Post text was not displayed.",
                waitForElementToBeDisplayedWithoutFailure(onView(withText(text))));

        return this;
    }
}
