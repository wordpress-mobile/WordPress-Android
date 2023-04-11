package org.wordpress.android.e2e.pages

import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase
import org.wordpress.android.support.WPSupportUtils

class ReaderViewPage {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val likerContainer = device.findObject(UiSelector().resourceId(buildResourceId("liker_faces_container")))
    private val relatedPostsContainer = device.findObject(
        UiSelector().resourceId(buildResourceId("container_related_posts"))
    )
    private val swipeForMore = device.findObject(UiSelector().textContains("Swipe for more"))
    private val recyclerView = UiScrollable(UiSelector().resourceId(buildResourceId("recycler_view")))
    private val savePostsForLater = device.findObject(UiSelector().text("Save Posts for Later"))
    private val okButton = device.findObject(UiSelector().text("OK"))
    private val bookmarkButtonSelector = UiSelector().resourceId(buildResourceId("bookmark"))

    private fun buildResourceId(id: String): String {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        return "$packageName:id/$id"
    }

    fun waitUntilLoaded(): ReaderViewPage {
        relatedPostsContainer.waitForExists(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        return this
    }

    fun likePost(): ReaderViewPage {
        tapLikeButton()
        likerContainer.waitForExists(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        return this
    }

    fun unlikePost(): ReaderViewPage {
        tapLikeButton()
        likerContainer.waitUntilGone(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        return this
    }

    private fun tapLikeButton() {
        swipeForMore.waitUntilGone(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        // Even though it was working locally in simulator, tapping the footer buttons,
        // like 'mLikeButton.click()', was not working in CI.
        // The current workaround is to use arrows navigation.

        // Bring focus to the footer. First button is selected.
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN)
        // Navigate to Like button.
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
        // Click the Like button.
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER)
        // Navigate back to the first footer button.
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
    }

    fun bookmarkPost(): ReaderViewPage {
        tapBookmarkButton()
        // Dismiss save posts locally dialog.
        if (savePostsForLater.exists()) {
            okButton.clickAndWaitForNewWindow()
        }
        return this
    }

    fun removeBookmarkPost(): ReaderViewPage {
        tapBookmarkButton()
        return this
    }

    private fun tapBookmarkButton() {
        // Scroll to the bookmark button.
        recyclerView.scrollIntoView(bookmarkButtonSelector)
        // Tap the bookmark button.
        device.findObject(bookmarkButtonSelector).clickAndWaitForNewWindow()
    }

    fun goBackToReader(): ReaderPage {
        device.pressBack()
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
        val isLiked = device
            .findObject(UiSelector().textContains("You like this."))
            .waitForExists(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        TestCase.assertTrue("Liker was not displayed.", isLiked)
        return this
    }

    fun verifyPostNotLiked(): ReaderViewPage {
        val likerDisplayed = likerContainer.exists()
        TestCase.assertFalse("Liker faces container was displayed.", likerDisplayed)
        return this
    }

    fun verifyPostBookmarked(): ReaderViewPage {
        val isBookmarked = device.findObject(UiSelector().text("Post saved"))
            .waitForExists(WPSupportUtils.DEFAULT_TIMEOUT.toLong())
        TestCase.assertTrue("Snackbar was not displayed.", isBookmarked)
        return this
    }

    fun verifyPostNotBookmarked(): ReaderViewPage {
        val isBookmarked = device.findObject(bookmarkButtonSelector).isSelected
        TestCase.assertFalse("The bookmark button is selected", isBookmarked)
        return this
    }
}
