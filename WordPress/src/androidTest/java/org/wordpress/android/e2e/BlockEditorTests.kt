package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.e2e.pages.BlockEditorPage
import org.wordpress.android.e2e.pages.MySitesPage
import org.wordpress.android.support.BaseTest
import java.time.Instant

@HiltAndroidTest
class BlockEditorTests : BaseTest() {
    @Before
    fun setUp() {
        logoutIfNecessary()
        wpLogin()
    }

    var mPostText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
    var mCategory = "Wedding"
    var mTag = "Tag " + Instant.now().toEpochMilli()
    var mHtmlPost = """<!-- wp:paragraph -->
<p>$mPostText</p>
<!-- /wp:paragraph -->
<div class="wp-block-column"><!-- wp:image {"id":65,"sizeSlug":"large"} -->
<figure class="wp-block-image size-large"><img src="https://fourpawsdoggrooming.files.wordpress.com/2020/08/image-1.jpg?w=731" alt="" class="wp-image-65"/></figure>
<!-- /wp:image --></div>
"""

    @Test
    @Ignore("Skipped due to increased flakiness. See build-and-ship channel for 17.05.2023")
    fun e2ePublishSimplePost() {
        val title = "publishSimplePost"
        MySitesPage()
            .startNewPost()
        BlockEditorPage()
            .waitForTitleDisplayed()
            .enterTitle(title)
            .enterParagraphText(mPostText)
            .publish()
            .verifyPostPublished()
    }

    @Test
    @Ignore("This test is temporarily disabled as being flaky.")
    fun e2ePublishFullPost() {
        val title = "publishFullPost"
        MySitesPage()
            .startNewPost()
        BlockEditorPage()
            .waitForTitleDisplayed()
            .enterTitle(title)
            .enterParagraphText(mPostText)
            .addImage()
            .addPostSettings(mCategory, mTag)
            .clickPublish()
            .verifyPostSettings(mCategory, mTag)
            .confirmPublish()
            .verifyPostPublished()
    }

    @Test
    fun e2eBlockEditorCanDisplayElementAddedInHtmlMode() {
        val title = "blockEditorCanDisplayElementAddedInHtmlMode"
        MySitesPage()
            .startNewPost()
        BlockEditorPage()
            .waitForTitleDisplayed()
            .enterTitle(title)
            .switchToHtmlMode()
            .enterParagraphText(mHtmlPost)
            .switchToVisualMode()
            .verifyPostElementText(mPostText)
    }
}
