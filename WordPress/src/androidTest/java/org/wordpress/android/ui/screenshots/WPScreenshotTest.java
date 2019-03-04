package org.wordpress.android.ui.screenshots;

import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.filters.LargeTest;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.PostsListPage;
import org.wordpress.android.e2e.pages.SitePickerPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.image.ImageType;

import java.util.function.Supplier;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;


import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.focusEditPostTitle;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.scrollToThenClickOn;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForConditionToBeTrue;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;
import static org.wordpress.android.support.WPSupportUtils.waitForSwipeRefreshLayoutToStopReloading;
@LargeTest
@RunWith(AndroidJUnit4.class)
public class WPScreenshotTest extends BaseTest {
    @ClassRule
    public static final WPLocaleTestRule LOCALE_TEST_RULE = new WPLocaleTestRule();


    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class,
            false, false);

    @Test
    public void wPScreenshotTest() {
        mActivityTestRule.launchActivity(null);
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        wpLogin();
        editBlogPost();
        manageMedia();
        navigateStats();
        navigateNotifications();
        wpLogout();
    }

    private void editBlogPost() {
        // Choose the "sites" tab in the nav
        clickOn(R.id.nav_sites);

        // Choose "Switch Site"
        clickOn(R.id.switch_site);

        (new SitePickerPage()).chooseSiteWithURL("infocusphotographers.com");

        // Get a screenshot of the post editor
        screenshotPostWithName("Summer Band Jam", "1-PostEditor", true);

        // Get a screenshot of the drafts feature
        screenshotPostWithName("Ideas", "5-DraftEditor", false);

        // Get a screenshot of the writing feature (without image)
        screenshotPostWithName("Book Your Summer Sessions", "6-Writing", true);

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void screenshotPostWithName(String name, String screenshotName, boolean hideKeyboard) {
        // Click on the "Blog Posts" row
        scrollToThenClickOn(R.id.row_blog_posts);

        // Wait for the blog posts to load, then edit the first post
        waitForSwipeRefreshLayoutToStopReloading();

        PostsListPage.tapPostWithName(name);

        // Wait for the editor to appear and load all images
        waitForElementToBeDisplayed(R.id.aztec);
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return editPostActivityIsNoLongerLoadingImages();
            }
        });

        // Click in the post title editor and ensure the caret is at the end of the title editor
        focusEditPostTitle();

        if (hideKeyboard) {
            Espresso.closeSoftKeyboard();
        }

        Screengrab.screenshot(screenshotName);
        pressBackUntilElementIsDisplayed(R.id.row_blog_posts);
    }

    private void manageMedia() {
        // Click on the "Sites" tab in the nav, then choose "Media"
        clickOn(R.id.nav_sites);
        clickOn(R.id.row_media);

        waitForElementToBeDisplayedWithoutFailure(R.id.media_grid_item_image);

        Screengrab.screenshot("4-media");
        pressBackUntilElementIsDisplayed(R.id.row_media);
    }

    private void navigateNotifications() {
        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);

        Screengrab.screenshot("3-notifications");

        // Exit the notifications activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateStats() {
        // Click on the "Sites" tab in the nav, then choose "Stats"
        clickOn(R.id.nav_sites);
        clickOn(R.id.row_stats);

        // Wait for the stats to load
        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.stats_block_list);
        Screengrab.screenshot("2-stats");

        // Exit the Stats Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private boolean editPostActivityIsNoLongerLoadingImages() {
        EditPostActivity editPostActivity = (EditPostActivity) getCurrentActivity();
        return editPostActivity.getAztecImageLoader().getNumberOfImagesBeingDownloaded() == 0;
    }
}
