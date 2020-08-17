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
import org.wordpress.android.e2e.pages.SitePickerPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.support.DemoModeEnabler;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.image.ImageType;

import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.dialogExistsWithTitle;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.getTranslatedString;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.scrollToThenClickOn;
import static org.wordpress.android.support.WPSupportUtils.selectItemWithTitleInTabLayout;
import static org.wordpress.android.support.WPSupportUtils.tapButtonInDialogWithTitle;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

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

        idleFor(1000);
        takeScreenshot("1-build-and-manage-your-website");

        editBlogPost();
        manageMedia();
        navigateNotifications();
        navigateStats();

        // Turn Demo Mode off on the emulator when we're done
        mDemoModeEnabler.disable();
        logoutIfNecessary();
    }

    private void editBlogPost() {
        // Choose the "sites" tab in the nav
        clickOn(R.id.nav_sites);

        // Choose "Switch Site"
        clickOn(R.id.switch_site);

        (new SitePickerPage()).chooseSiteWithURL("infocusphotographers.com");

        // Choose "Blog Posts"
        scrollToThenClickOn(R.id.quick_action_posts_button);

        // Choose "Drafts"
        selectItemWithTitleInTabLayout(getTranslatedString(R.string.post_list_tab_drafts), R.id.tabLayout);

        // Get a screenshot of the writing feature (without image)
        String name = "2-create-beautiful-posts-and-pages";
        screenshotPostWithName("Time to Book Summer Sessions", name, false);

        // Get a screenshot of the drafts feature
        screenshotPostWithName("Ideas", "6-capture-ideas-on-the-go", false);

        // Get a screenshot of the drafts feature
        screenshotPostWithName("Summer Band Jam", "7-create-beautiful-posts-and-pages", true);

        // Get a screenshot for "write without compromises"
        screenshotPostWithName("Now Booking Summer Sessions", "8-write-without-compromises", true);

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void screenshotPostWithName(String name, String screenshotName, boolean hideKeyboard) {
        idleFor(2000);

        PostsListPage.scrollToTop();
        PostsListPage.tapPostWithName(name);

        if (dialogExistsWithTitle(getTranslatedString(R.string.dialog_gutenberg_informative_title))) {
            tapButtonInDialogWithTitle(getTranslatedString(R.string.dialog_button_ok));
        }


        waitForElementToBeDisplayed(R.id.editor_activity);

        // Wait for the editor to load all images
        idleFor(5000);

        if (hideKeyboard) {
            Espresso.closeSoftKeyboard();
        }

        takeScreenshot(screenshotName);
        pressBackUntilElementIsDisplayed(R.id.tabLayout);
    }

    private void manageMedia() {
        // Click on the "Sites" tab in the nav, then choose "Media"
        clickOn(R.id.nav_sites);
        clickOn(R.id.quick_action_media_button);

        waitForElementToBeDisplayedWithoutFailure(R.id.media_grid_item_image);

        takeScreenshot("5-share-from-anywhere");

        pressBackUntilElementIsDisplayed(R.id.quick_action_media_button);
    }

    private void navigateNotifications() {
        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);


        takeScreenshot("4-check-whats-happening-in-real-time");

        // Exit the notifications activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateStats() {
        // Click on the "Sites" tab in the nav, then choose "Stats"
        clickOn(R.id.nav_sites);
        clickOn(R.id.row_stats);

        // Show the year view – it'll have the best layout
        selectItemWithTitleInTabLayout(getTranslatedString(R.string.stats_timeframe_years), R.id.tabLayout);

        // Wait for the stats to load
        idleFor(5000);

        takeScreenshot("3-track-what-your-visitors-love");

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
