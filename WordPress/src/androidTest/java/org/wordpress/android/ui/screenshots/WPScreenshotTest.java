package org.wordpress.android.ui.screenshots;

import android.provider.Settings;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.google.android.libraries.cloudtesting.screenshots.ScreenShotter;

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
import static org.wordpress.android.support.WPSupportUtils.clickOnViewWithTag;
import static org.wordpress.android.support.WPSupportUtils.dialogExistsWithTitle;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.getTranslatedString;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.longClickOn;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.scrollToThenClickOn;
import static org.wordpress.android.support.WPSupportUtils.selectItemWithTitleInTabLayout;
import static org.wordpress.android.support.WPSupportUtils.setNightMode;
import static org.wordpress.android.support.WPSupportUtils.swipeUpOnView;
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

        editBlogPost();
        navigateDiscover();
        navigateMySite();
        navigateStats();
        navigateNotifications();
        manageMedia();

        // Turn Demo Mode off on the emulator when we're done
        mDemoModeEnabler.disable();
        logoutIfNecessary();
    }

    private void editBlogPost() {
        setNightMode(false);

        // Choose the "sites" tab in the nav
        clickOn(R.id.nav_sites);

        // Choose "Switch Site"
        clickOn(R.id.switch_site);

        (new SitePickerPage()).chooseSiteWithURL("fourpawsdoggrooming.wordpress.com");

        // Choose "Blog Posts"
        scrollToThenClickOn(R.id.quick_action_posts_button);

        // Choose "Drafts"
        selectItemWithTitleInTabLayout(getTranslatedString(R.string.post_list_tab_drafts), R.id.tabLayout);

        // Get a screenshot of the editor with the block library expanded
        String name = "1-create-a-site-or-start-a-blog";

        screenshotPostWithName("Our Services", name, false, true);

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void screenshotPostWithName(String name,
                                        String screenshotName,
                                        boolean hideKeyboard,
                                        boolean openBlockList) {
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

        if (openBlockList) {
            clickOnViewWithTag("add-block-button");
        }

        takeScreenshot(screenshotName);
        pressBackUntilElementIsDisplayed(R.id.tabLayout);
    }

    private void navigateDiscover() {
        setNightMode(true);

        // Click on the "Reader" tab and take a screenshot
        clickOn(R.id.nav_reader);

        waitForElementToBeDisplayedWithoutFailure(R.id.interests_fragment_container);

        swipeUpOnView(R.id.interests_fragment_container, (float) 1.15);
        swipeUpOnView(R.id.fragment_container, (float) 0.5);

        takeScreenshot("2-discover-new-reads");

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateStats() {
        setNightMode(false);
        // Click on the "Sites" tab in the nav, then choose "Stats"
        clickOn(R.id.nav_sites);
        clickOn(R.id.row_stats);

        // Show the months view
        selectItemWithTitleInTabLayout(getTranslatedString(R.string.stats_timeframe_months), R.id.tabLayout);

        // Wait for the stats to load
        idleFor(5000);

        takeScreenshot("3-build-an-audience");

        // Exit the Stats Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateMySite() {
        setNightMode(true);
        // Click on the "Sites" tab and take a screenshot
        clickOn(R.id.nav_sites);

        // Choose "Switch Site"
        clickOn(R.id.switch_site);

        (new SitePickerPage()).chooseSiteWithURL("tricountyrealestate.wordpress.com");

        waitForElementToBeDisplayedWithoutFailure(R.id.row_blog_posts);

        if (isElementDisplayed(R.id.tooltip_message)) {
            clickOn(R.id.tooltip_message);
        }

        takeScreenshot("4-keep-tabs-on-your-site");
    }

    private void navigateNotifications() {
        setNightMode(false);
        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);


        takeScreenshot("5-reply-in-real-time");

        // Exit the notifications activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void manageMedia() {
        setNightMode(true);

        // Click on the "Sites" tab in the nav, then choose "Media"
        clickOn(R.id.nav_sites);
        clickOn(R.id.quick_action_media_button);

        waitForElementToBeDisplayedWithoutFailure(R.id.media_grid_item_image);

        takeScreenshot("6-upload-on-the-go");

        pressBackUntilElementIsDisplayed(R.id.quick_action_media_button);
    }

    private void takeScreenshot(String screenshotName) {
        try {
            if (runningInTestLab()) {
                ScreenShotter.takeScreenshot(screenshotName, getCurrentActivity());
            } else {
                // Fallback to screengrab
                Screengrab.screenshot(screenshotName);
            }
        } catch (RuntimeException r) {
            // Screenshots will fail when running outside of Fastlane or FTL, so this is safe to ignore.
        }
    }

    private boolean runningInTestLab() {
        // https://firebase.google.com/docs/test-lab/android/android-studio#modify_instrumented_test_behavior_for
        String testLabSetting = Settings.System.getString(
                getCurrentActivity().getContentResolver(),
                "firebase.test.lab"
        );
        return "true".equals(testLabSetting);
    }

    private boolean editPostActivityIsNoLongerLoadingImages() {
        EditPostActivity editPostActivity = (EditPostActivity) getCurrentActivity();
        return editPostActivity.getAztecImageLoader().getNumberOfImagesBeingDownloaded() == 0;
    }
}
