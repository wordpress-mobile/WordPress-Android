package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.pages.BlockEditorPage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.support.BaseTest;

import java.time.Instant;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class BlockEditorTests extends BaseTest {
    @Rule
    public GrantPermissionRule mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();
    }

    String mPostText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    String mCategory = "Wedding";
    String mTag = "Tag " + Instant.now().toEpochMilli();
    String mHtmlPost = "<!-- wp:paragraph -->\n"
                       + "<p>" + mPostText + "</p>\n"
                       + "<!-- /wp:paragraph -->"
                       + "\n"
                       + "<div class=\"wp-block-column\"><!-- wp:image {\"id\":65,\"sizeSlug\":\"large\"} -->\n"
                       + "<figure class=\"wp-block-image size-large\"><img src=\"https://fourpawsdoggrooming.files"
                       + ".wordpress.com/2020/08/image-1.jpg?w=731\" alt=\"\" class=\"wp-image-65\"/></figure>\n"
                       + "<!-- /wp:image --></div>\n";

    @Test
    public void publishSimplePost() {
        String title = "publishSimplePost";

        new MySitesPage()
                .go()
                .startNewPost();

        new BlockEditorPage()
                .waitForTitleDisplayed()
                .enterTitle(title)
                .enterParagraphText(mPostText)
                .publish()
                .verifyPostPublished();
    }

    @Test
    public void publishFullPost() {
        String title = "publishFullPost";

        new MySitesPage()
                .go()
                .startNewPost();

        new BlockEditorPage()
                .waitForTitleDisplayed()
                .enterTitle(title)
                .enterParagraphText(mPostText)
                .addImage()
                .addPostSettings(mCategory, mTag)
                .clickPublish()
                .verifyPostSettings(mCategory, mTag)
                .confirmPublish()
                .verifyPostPublished();
    }

    @Test
    public void blockEditorCanDisplayElementAddedInHtmlMode() {
        String title = "blockEditorCanDisplayElementAddedInHtmlMode";

        new MySitesPage()
                .go()
                .startNewPost();

        new BlockEditorPage()
                .waitForTitleDisplayed()
                .enterTitle(title)
                .switchToHtmlMode()
                .enterParagraphText(mHtmlPost)
                .switchToVisualMode()
                .verifyPostElementText(mPostText);
    }
}
