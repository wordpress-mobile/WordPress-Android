package org.wordpress.android.ui.screenshots;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.PostsListPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.support.DemoModeEnabler;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.image.ImageType;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.scrollToThenClickOn;
import static org.wordpress.android.support.WPSupportUtils.selectItemWithTitleInTabLayout;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WPScreenshotTest extends BaseTest {
    @ClassRule
    public static final WPLocaleTestRule LOCALE_TEST_RULE = new WPLocaleTestRule();


    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class,
            false, false);

    private DemoModeEnabler mDemoModeEnabler = new DemoModeEnabler();

    @Test
    public void wPScreenshotTest() {
        mActivityTestRule.launchActivity(null);
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        // Enable Demo Mode
        mDemoModeEnabler.enable();

        wpLogin();

        editBlogPost();
        manageMedia();
        navigateStats();
        navigateNotifications();

        // Turn Demo Mode off on the emulator when we're done
        mDemoModeEnabler.disable();
    }

    private void editBlogPost() {
        // Get a screenshot of the post editor
        screenshotPostWithName("Summer Band Jam", "1-PostEditor", true);

        // Get a screenshot of the drafts feature
        screenshotPostWithName("Ideas", "5-DraftEditor", false);

        // Get a screenshot of the writing feature (without image)
        screenshotPostWithName("Time to Book Summer Sessions", "6-Writing", true);

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void screenshotPostWithName(String name, String screenshotName, boolean hideKeyboard) {
        // Click on the "Blog Posts" row
        scrollToThenClickOn(R.id.row_blog_posts);

        // Wait for the blog posts to load, then edit the first post
        selectItemWithTitleInTabLayout("Drafts", R.id.tabLayout);

        idleFor(2000);

        PostsListPage.tapPostWithName(name);

        waitForElementToBeDisplayed(R.id.editor_activity);

        // Wait for the editor to load all images
        idleFor(5000);

        if (hideKeyboard) {
            Espresso.closeSoftKeyboard();
        }

        takeScreenshot(screenshotName);
        pressBackUntilElementIsDisplayed(R.id.row_blog_posts);
    }

    private void manageMedia() {
        // Click on the "Sites" tab in the nav, then choose "Media"
        clickOn(R.id.nav_sites);
        clickOn(R.id.row_media);

        waitForElementToBeDisplayedWithoutFailure(R.id.media_grid_item_image);

        takeScreenshot("4-media");
        pressBackUntilElementIsDisplayed(R.id.row_media);
    }

    private void navigateNotifications() {
        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);

        takeScreenshot("3-notifications");

        // Exit the notifications activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateStats() {
        // Click on the "Sites" tab in the nav, then choose "Stats"
        clickOn(R.id.nav_sites);
        clickOn(R.id.row_stats);

        // Wait for the stats to load
        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.stats_block_list);
        takeScreenshot("2-stats");

        // Exit the Stats Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void takeScreenshot(String screenshotName) {
        try {
            Screengrab.screenshot(screenshotName);
        } catch (RuntimeException r) {
            // Screenshots will fail when running outside of Fastlane, so this is safe to ignore.
        }
    }

    private boolean editPostActivityIsNoLongerLoadingImages() {
        EditPostActivity editPostActivity = (EditPostActivity) getCurrentActivity();
        return editPostActivity.getAztecImageLoader().getNumberOfImagesBeingDownloaded() == 0;
    }
}
