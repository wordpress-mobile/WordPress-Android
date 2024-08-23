package org.wordpress.android.ui.screenshots;

import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.filters.LargeTest;

import com.google.android.libraries.cloudtesting.screenshots.ScreenShotter;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.e2e.pages.PostsListPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.support.DemoModeEnabler;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.UiTestingUtils;
import org.wordpress.android.util.image.ImageType;

import dagger.hilt.android.testing.HiltAndroidTest;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.clickOnViewWithTag;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.isTabletScreen;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.setNightMode;
import static org.wordpress.android.support.WPSupportUtils.swipeDownOnView;
import static org.wordpress.android.support.WPSupportUtils.swipeUpOnView;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;

@LargeTest
@HiltAndroidTest
public class WPScreenshotTest extends BaseTest {
    @ClassRule
    public static final RuleChain LOCALE_TEST_RULES = RuleChain
            // Run fastlane's official LocaleTestRule (which switches device language + sets up screengrab) first
            .outerRule(new LocaleTestRule())
            // Run our own rule (which handles our in-app locale switching logic) second
            .around(new WPLocaleTestRule());

    // Note: running this as a static @ClassRule as part of the above RuleChain doesn't seem to work
    // (apparently that would make those run too early?), but running it as @Rule does fix the issue.
    // Since we only have one test case in that test class (and that the code to change the IME is fast),
    // that shouldn't really be problem in practice.
    @Rule
    public ImeTestRule IME_TEST_RULE = new ImeTestRule();

    private DemoModeEnabler mDemoModeEnabler = new DemoModeEnabler();

    @Test
    public void wPScreenshotTest() {
        if (!BuildConfig.IS_JETPACK_APP) {
            ActivityScenario.launch(WPLaunchActivity.class);
            Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

            // Enable Demo Mode
            mDemoModeEnabler.enable();

            setLightModeAndWait();

            wpLogin();

            // Even though the screenshot for edit post is captured without error,
            // wiremock sometimes still throws a VerificationException which
            // in turn causes our ci process to fail instrumentation tests.
            // For the time being, editBlogPost is going to be commented out
            // editBlogPost();
            navigateDiscover();
            navigateMySite();
            navigateStats();
            navigateNotifications();
            manageMedia();

            // Turn Demo Mode off on the emulator when we're done
            mDemoModeEnabler.disable();
            logoutIfNecessary();
        }
    }

    private void editBlogPost() {
        (new MySitesPage()).switchToSite("fourpawsdoggrooming.wordpress.com")
                           .goToPosts();

        // There is a possibility of the edit post getting stuck with an `AppNotIdleException`
        // On the UI it's shown by the flashing progress indicator at the bottom. This idle
        // appears to wait long enough for the edit screen to show properly
        idleFor(3000);

        PostsListPage.goToDrafts();

        // Get a screenshot of the editor with the block library expanded
        String name = "1-create-a-site-or-start-a-blog";

        // Wait for the editor to load all images
        idleFor(5000);

        screenshotPostWithName("Our Services", name, false, !isTabletScreen());

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

        waitForElementToBeDisplayed(R.id.editor_activity);

        // Wait for the editor to load all images
        idleFor(7000);

        if (hideKeyboard) {
            Espresso.closeSoftKeyboard();
        }

        if (openBlockList) {
            clickOnViewWithTag("add-block-button");
            idleFor(2000);
        }

        takeScreenshot(screenshotName);
        pressBackUntilElementIsDisplayed(R.id.tabLayout);
    }

    private void navigateDiscover() {
        (new MySitesPage()).switchToSite("fourpawsdoggrooming.wordpress.com");

        // Click on the "Reader" tab and take a screenshot
        clickOn(R.id.nav_reader);

        waitForElementToBeDisplayedWithoutFailure(R.id.interests_fragment_container);

        idleFor(2000);

        swipeUpOnView(R.id.interests_fragment_container, (float) 1.15);

        swipeUpOnView(R.id.fragment_container, (float) 0.5);

        idleFor(2000);

        // Workaround to avoid gray overlay
        try {
            UiTestingUtils.swipeToAvoidGrayOverlayIgnoringFailures(R.id.view_pager);
            if (isTabletScreen()) {
                swipeDownOnView(R.id.view_pager, (float) 0.5);
                idleFor(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Wait for the editor to load all images
        idleFor(7000);

        takeScreenshot("2-discover-new-reads");

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateStats() {
        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Stats"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToStats();

        UiTestingUtils.swipeToAvoidGrayOverlayIgnoringFailures(R.id.statsPager);

        if (isElementDisplayed(R.id.button_negative)) {
            clickOn(R.id.button_negative);
        }

        takeScreenshot("3-build-an-audience");

        // Exit the Stats Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateMySite() {
        (new MySitesPage()).switchToSite("tricountyrealestate.wordpress.com");

        waitForElementToBeDisplayedWithoutFailure(R.id.recycler_view);

        takeScreenshot("4-keep-tabs-on-your-site");
    }

    private void navigateNotifications() {
        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);

        // Wait for the images to load
        idleFor(6000);

        takeScreenshot("5-reply-in-real-time");

        // Exit the notifications activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void manageMedia() {
        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Media"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToMedia();

        waitForElementToBeDisplayedWithoutFailure(R.id.media_browser_container);

        idleFor(2000);

        takeScreenshot("6-upload-on-the-go");

        pressBackUntilElementIsDisplayed(R.id.nav_sites);
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

    private void setLightModeAndWait() {
        setNightMode(false);
        idleFor(5000);
    }
}
