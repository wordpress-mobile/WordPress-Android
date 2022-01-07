package org.wordpress.android.e2e.pages;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import static junit.framework.TestCase.assertTrue;
import static org.wordpress.android.support.WPSupportUtils.DEFAULT_TIMEOUT;
import static org.wordpress.android.support.WPSupportUtils.isTextDisplayed;

public class ReaderViewPage {
    UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    BySelector mLikerSelector = By.res("org.wordpress.android.prealpha:id/liker_faces_container");
    BySelector mLikeCountSelector = By.res("org.wordpress.android.prealpha:id/text_count");
    BySelector mRelatedPostsContainer = By.res("org.wordpress.android.prealpha:id/container_related_posts");

    public ReaderViewPage waitUntilLoaded() {
        mDevice.wait(Until.hasObject(mRelatedPostsContainer), DEFAULT_TIMEOUT);

        return this;
    }

    public ReaderViewPage like() {
        tapLikeButton();
        mDevice.wait(Until.hasObject(mLikerSelector), DEFAULT_TIMEOUT);

        return this;
    }

    public ReaderViewPage unlike() {
        tapLikeButton();
        mDevice.wait(Until.gone(mLikerSelector), DEFAULT_TIMEOUT);

        return this;
    }

    private void tapLikeButton() {
        UiObject likeButton =
                mDevice.findObject(new UiSelector().resourceId("org.wordpress.android.prealpha:id/count_likes"));

        try {
            likeButton.click();
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

    public ReaderViewPage verifyPostLiked() {
        boolean likerDisplayed = !mDevice.findObjects(mLikerSelector).isEmpty();
        boolean postHasOneLike = mDevice.findObject(mLikeCountSelector).getText().equals("1");

        assertTrue("Liker was not displayed.", likerDisplayed);
        assertTrue("Like count was different from '1'.", postHasOneLike);

        return this;
    }

    public ReaderViewPage verifyPostNotLiked() {
        boolean likerNotDisplayed = mDevice.findObjects(mLikerSelector).isEmpty();

        assertTrue("Liker faces container was not displayed.", likerNotDisplayed);

        return this;
    }
}
