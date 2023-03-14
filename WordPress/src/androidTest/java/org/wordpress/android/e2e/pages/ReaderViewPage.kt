package org.wordpress.android.e2e.pages

import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase
import org.wordpress.android.support.WPSupportUtils

class ReaderViewPage {
    var mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    var mLikerContainerId = "org.wordpress.android.prealpha:id/liker_faces_container"
    var mRelatedPostsId = "org.wordpress.android.prealpha:id/container_related_posts"
    var mFooterId = "org.wordpress.android.prealpha:id/layout_post_detail_footer"
    var mLikerContainer = mDevice.findObject(UiSelector().resourceId(mLikerContainerId))
    var mRelatedPostsContainer = mDevice.findObject(UiSelector().resourceId(mRelatedPostsId))
    var mSwipeForMore = mDevice.findObject(UiSelector().textContains("Swipe for more"))
    var mFooter = mDevice.findObject(UiSelector().resourceId(mFooterId))
    fun waitUntilLoaded(): ReaderViewPage {
        mRelatedPostsContainer.waitForExists(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        return this
    }

    fun likePost(): ReaderViewPage {
        tapLikeButton()
        mLikerContainer.waitForExists(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        return this
    }

    fun unlikePost(): ReaderViewPage {
        tapLikeButton()
        mLikerContainer.waitUntilGone(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        return this
    }

    private fun tapLikeButton() {
        mSwipeForMore.waitUntilGone(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        // Even though it was working locally in simulator, tapping the footer buttons,
        // like 'mLikeButton.click()', was not working in CI.
        // The current workaround is to use arrows navigation.

        // Bring focus to the footer. First button is selected.
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN)
        // Navigate to Like button.
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
        // Click the Like button.
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER)
        // Navigate back to the first footer button.
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
    }

    fun goBackToReader(): ReaderPage {
        mDevice.pressBack()
        return ReaderPage()
    }

    fun slideToPreviousPost(): ReaderViewPage {
        WPSupportUtils.swipeToRight()
        return this
    }

    fun slideToNextPost(): ReaderViewPage {
        WPSupportUtils.swipeToLeft()
        return this
    }

    fun verifyPostDisplayed(title: String): ReaderViewPage {
        TestCase.assertTrue(
            "Post title was not displayed. Target post: $title",
            WPSupportUtils.isTextDisplayed(title)
        )
        return this
    }

    fun verifyPostLiked(): ReaderViewPage {
        val isLiked = mDevice
            .findObject(UiSelector().textContains("You like this."))
            .waitForExists(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        TestCase.assertTrue("Liker was not displayed.", isLiked)
        return this
    }

    fun verifyPostNotLiked(): ReaderViewPage {
        val likerDisplayed = mLikerContainer.exists()
        TestCase.assertFalse("Liker faces container was displayed.", likerDisplayed)
        return this
    }
}
