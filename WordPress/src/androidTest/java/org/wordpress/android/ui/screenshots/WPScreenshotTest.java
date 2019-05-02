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
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.image.ImageType;


import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;


import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.scrollToThenClickOn;
import static org.wordpress.android.support.WPSupportUtils.selectItemWithTitleInTabLayout;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;
import static org.wordpress.android.test.BuildConfig.SCREENSHOT_LOGINPASSWORD;
import static org.wordpress.android.test.BuildConfig.SCREENSHOT_LOGINUSERNAME;

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
        
        // Never show the Gutenberg dialog when opening a post
        AppPrefs.setGutenbergInformativeDialogDisabled(true);

        tmpWPLogin();
        editBlogPost();
        manageMedia();
        navigateStats();
        navigateNotifications();
        tmpWpLogout();
    }

    private void tmpWPLogin() {
        // If we're already logged in, log out before starting
        if (!isElementDisplayed(R.id.login_button)) {
            this.tmpWpLogout();
        }

        // Login Prologue – We want to log in, not sign up
        // See LoginPrologueFragment
        clickOn(R.id.login_button);

        // Email Address Screen – Fill it in and click "Next"
        // See LoginEmailFragment
        populateTextField(R.id.input, SCREENSHOT_LOGINUSERNAME);
        clickOn(R.id.primary_button);

        // Receive Magic Link or Enter Password Screen – Choose "Enter Password"
        // See LoginMagicLinkRequestFragment
        clickOn(R.id.login_enter_password);

        // Password Screen – Fill it in and click "Next"
        // See LoginEmailPasswordFragment
        populateTextField(R.id.input, SCREENSHOT_LOGINPASSWORD);
        clickOn(R.id.primary_button);

        // Login Confirmation Screen – Click "Continue"
        // See LoginEpilogueFragment
        clickOn(R.id.primary_button);
    }

    private void tmpWpLogout() {
        // Click on the "Me" tab in the nav, then choose "Log Out"
        clickOn(R.id.nav_me);
        scrollToThenClickOn(R.id.row_logout);

        // Confirm that we want to log out
        clickOn(android.R.id.button1);
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
