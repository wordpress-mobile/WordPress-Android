package org.wordpress.android.e2e.pages;

import android.view.KeyEvent;

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

    String mLikeButtonId = "org.wordpress.android.prealpha:id/count_likes";
    String mLikerContainerId = "org.wordpress.android.prealpha:id/liker_faces_container";
    String mRelatedPostsId = "org.wordpress.android.prealpha:id/container_related_posts";

    UiObject mLikeButton = mDevice.findObject(new UiSelector().resourceId(mLikeButtonId));
    UiObject mLikerContainer = mDevice.findObject(new UiSelector().resourceId(mLikerContainerId));
    UiObject mRelatedPostsContainer = mDevice.findObject(new UiSelector().resourceId(mRelatedPostsId));
    UiObject mSwipeForMore = mDevice.findObject(new UiSelector().textContains("Swipe for more"));

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
            mSwipeForMore.waitUntilGone(DEFAULT_TIMEOUT);
            // Even though it was working locally in simulator, tapping the footer buttons like 'mLikeButton.click()'
            // was not working in CI with same settings Pixel 2 API 28 Android 9.0.
            // The current workaround is to use arrows navigation.
            mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
            mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
            mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
            mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
            mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER);
            // Click somewhere remove focus
            mDevice.click(10, 10);
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
        String likeButtonDescription = mLikeButton.getContentDescription();


        assertTrue("Liker was not displayed.", likerDisplayed);
        assertTrue("Like button content description was different from 'You like this'.",
                likeButtonDescription.equals("You like this"));

        return this;
    }

    public ReaderViewPage verifyPostNotLiked() {
        boolean likerDisplayed = mLikerContainer.exists();

        assertFalse("Liker faces container was displayed.", likerDisplayed);

        return this;
    }
}
