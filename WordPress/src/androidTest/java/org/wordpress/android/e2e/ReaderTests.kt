package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.e2e.pages.ReaderPage
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class ReaderTests : BaseTest() {
    @Before
    fun setUp() {
        logoutIfNecessary()
        wpLogin()
        ReaderPage().go()
    }

    @Test
    @Ignore("Skipped due to increased flakiness. See build-and-ship channel for 17.05.2023")
    fun e2eNavigateThroughPosts() {
        ReaderPage()
            .tapFollowingTab()
            .openBlogOrPost(TITLE_COACHING_POST)
            .verifyPostDisplayed(TITLE_COACHING_POST)
            .slideToPreviousPost()
            .verifyPostDisplayed(TITLE_COMPETITION_POST)
            .slideToNextPost()
            .verifyPostDisplayed(TITLE_COACHING_POST)
            .goBackToReader()
    }

    @Test
    @Ignore("Skipped due to increased flakiness. See build-and-ship channel for 17.05.2023")
    fun e2eLikePost() {
        ReaderPage()
            .tapFollowingTab()
            .openBlogOrPost(TITLE_COACHING_POST)
            .likePost()
            .verifyPostLiked()
            .unlikePost()
            .verifyPostNotLiked()
            .goBackToReader()
    }

    @Test
    fun e2eBookmarkPost() {
        ReaderPage()
            .tapFollowingTab()
            .openBlogOrPost(TITLE_LONGREADS_BLOG)
            .bookmarkPost()
            .verifyPostBookmarked()
            .removeBookmarkPost()
            .verifyPostNotBookmarked()
            .goBackToReader()
    }

    companion object {
        private const val TITLE_LONGREADS_BLOG = "Longreads"
        private const val TITLE_COACHING_POST = "Let's check out the coaching team!"
        private const val TITLE_COMPETITION_POST = "Let's focus on the competition."
    }
}
