package org.wordpress.android.e2e.pages;

import android.view.KeyEvent;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.wordpress.android.support.WPSupportUtils.DEFAULT_TIMEOUT;
import static org.wordpress.android.support.WPSupportUtils.isTextDisplayed;
import static org.wordpress.android.support.WPSupportUtils.swipeToLeft;
import static org.wordpress.android.support.WPSupportUtils.swipeToRight;

public class ReaderViewPage {
    UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    String mTextTitleContainerId = "org.wordpress.android.prealpha:id/text_title";
    String mLikeButtonId = "org.wordpress.android.prealpha:id/count_likes";
    String mLikerContainerId = "org.wordpress.android.prealpha:id/liker_faces_container";
    String mRelatedPostsId = "org.wordpress.android.prealpha:id/container_related_posts";
    String mHeaderId = "org.wordpress.android.prealpha:id/header_view";

    UiObject mTextTitleContainer = mDevice.findObject(new UiSelector().resourceId(mTextTitleContainerId));
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
        mSwipeForMore.waitUntilGone(DEFAULT_TIMEOUT);
        // Even though it was working locally in simulator, tapping the footer buttons,
        // like 'mLikeButton.click()', was not working in CI.
        // The current workaround is to use arrows navigation.
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER);
        // Click somewhere to remove focus
        resetFocus();
    }

    private void resetFocus() {
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
    }

    public ReaderPage goBackToReader() {
        mDevice.pressBack();

        return new ReaderPage();
    }

    public ReaderViewPage slideToPreviousPost() {
        swipeToRight();

        return this;
    }

    public ReaderViewPage slideToNextPost() {
        swipeToLeft();

        return this;
    }

    public ReaderViewPage verifyPostDisplayed(String title, String content) {
        assertTrue("Post title was not displayed", isTextDisplayed(title));
        assertTrue("Post content was not displayed.", isTextDisplayed(content));

        return this;
    }

    public ReaderViewPage verifyPostLiked() {
        boolean isLiked = mDevice
                .findObject(new UiSelector().textContains("You like this.")).waitForExists(DEFAULT_TIMEOUT);

        assertTrue("Liker was not displayed.", isLiked);

        return this;
    }

    public ReaderViewPage verifyPostNotLiked() {
        boolean likerDisplayed = mLikerContainer.exists();

        assertFalse("Liker faces container was displayed.", likerDisplayed);

        return this;
    }
}
