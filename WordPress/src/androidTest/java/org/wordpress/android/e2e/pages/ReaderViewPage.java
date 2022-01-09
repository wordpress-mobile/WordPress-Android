package org.wordpress.android.e2e.pages;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.wordpress.android.support.WPSupportUtils.DEFAULT_TIMEOUT;
import static org.wordpress.android.support.WPSupportUtils.isTextDisplayed;

public class ReaderViewPage {
    UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    UiObject mLikeButton =
            mDevice.findObject(new UiSelector().resourceId("org.wordpress.android.prealpha:id/count_likes"));
    UiObject mLikerContainer =
            mDevice.findObject(new UiSelector().resourceId("org.wordpress.android.prealpha:id/liker_faces_container"));
    UiObject mLikeCountSelector =
            mDevice.findObject(new UiSelector().resourceId("org.wordpress.android.prealpha:id/text_count"));
    UiObject mRelatedPostsContainer =
            mDevice.findObject(new UiSelector().resourceId("org.wordpress.android.prealpha:id/container_related_posts"));

    public ReaderViewPage waitUntilLoaded() {
        mRelatedPostsContainer.waitForExists(DEFAULT_TIMEOUT);

        return this;
    }

    public ReaderViewPage like() {
        tapLikeButton();
        mLikerContainer.waitForExists(DEFAULT_TIMEOUT);

        return this;
    }

    public ReaderViewPage unlike() {
        tapLikeButton();
        mLikerContainer.waitUntilGone(DEFAULT_TIMEOUT);

        return this;
    }

    private void tapLikeButton() {
        try {
            mLikeButton.click();
        } catch (Exception e) {
            // Ignore
        }
    }

    public ReaderPage goBackToReader() {
        mDevice.pressBack();

        return new ReaderPage();
    }

    public ReaderViewPage verifyPostDisplayed(String title, String text) {
        assertTrue("Post title was not displayed", isTextDisplayed(title));
        assertTrue("Post text was not displayed.", isTextDisplayed(text));

        return this;
    }

    public ReaderViewPage verifyPostLiked() throws UiObjectNotFoundException {
        boolean likerDisplayed = mLikerContainer.exists();
        boolean postHasOneLike = mLikeCountSelector.getText().equals("1");

        assertTrue("Liker was not displayed.", likerDisplayed);
        assertTrue("Like count was different from '1'.", postHasOneLike);

        return this;
    }

    public ReaderViewPage verifyPostNotLiked() {
        boolean likerDisplayed = mLikerContainer.exists();

        assertFalse("Liker faces container was displayed.", likerDisplayed);

        return this;
    }
}
