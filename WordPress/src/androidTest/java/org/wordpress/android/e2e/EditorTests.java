package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.EditorPage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.e2e.pages.SiteSettingsPage;
import org.wordpress.android.support.BaseTest;

import java.time.Instant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertTrue;
import static org.wordpress.android.support.WPSupportUtils.checkViewHasText;
import static org.wordpress.android.support.WPSupportUtils.sleep;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToNotBeDisplayed;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class EditorTests extends BaseTest {
    @Rule
    public GrantPermissionRule mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();

        MySitesPage mySitesPage = new MySitesPage().go();
        sleep();

        mySitesPage.goToSettings();

        // Set to Classic.
        new SiteSettingsPage().setEditorToClassic();

        // exit the Settings page
        pressBack();

        mySitesPage.goToPosts();

        mySitesPage.startNewPost();
    }

    // For more info see Issue: https://github.com/wordpress-mobile/WordPress-Android/issues/14389
    @Ignore("Classic Editor being deprecated for new posts, test should be adjusted to editing existing classic post")
    @Test
    public void e2ePublishSimplePostClassic() {
        String title = "Hello Espresso!";
        String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

        EditorPage editorPage = new EditorPage();
        editorPage.enterTitle(title);
        editorPage.enterContent(content);
        boolean isPublished = editorPage.publishPost();
        assertTrue(isPublished);
    }

    // For more info see Issue: https://github.com/wordpress-mobile/WordPress-Android/issues/14389
    @Ignore("Classic Editor being deprecated for new posts, test should be adjusted to editing existing classic post")
    @Test
    public void e2ePublishFullPostClassic() {
        String title = "Hello Espresso!";
        String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod "
                         + "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud "
                         + "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.";
        String category = "Wedding";
        long now = Instant.now().toEpochMilli();
        String tag = "Tag " + now;

        EditorPage editorPage = new EditorPage();
        editorPage.enterTitle(title);
        editorPage.enterContent(content);
        editorPage.enterImage();
        editorPage.openSettings();

        editorPage.addACategory(category);
        editorPage.addATag(tag);
        editorPage.setFeaturedImage();

        // ----------------------------
        // Verify post settings data
        // ----------------------------
        // Verify Category added
        checkViewHasText(onView(withId(R.id.post_categories)), category);

        // Verify tag added
        checkViewHasText(onView(withId(R.id.post_tags)), tag);

        // Verify the featured image added
        waitForElementToNotBeDisplayed(onView(withText(R.string.post_settings_set_featured_image)));

        // head back to the post
        pressBack();

        // publish
        boolean isPublished = editorPage.publishPost();
        assertTrue(isPublished);
    }
}
