package org.wordpress.android.ui.screenshots;

import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
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
import org.wordpress.android.util.UiTestingUtils;
import org.wordpress.android.util.image.ImageType;

import java.util.Locale;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.wordpress.android.support.WPSupportUtils.childAtPosition;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.clickOnViewWithTag;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.getTranslatedString;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.isTabletScreen;
import static org.wordpress.android.support.WPSupportUtils.isTextDisplayed;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.setNightMode;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;

import dagger.hilt.android.testing.HiltAndroidTest;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@LargeTest
@HiltAndroidTest
public class JPScreenshotTest extends BaseTest {
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

    private static final String JETPACK_SCREENSHOT_SITE_URL = "yourjetpack.blog";

    private DemoModeEnabler mDemoModeEnabler = new DemoModeEnabler();

    public enum Screenshots {
        ACTIVITY_LOG(false, "activity-log", 0),
        BACKUP_DOWNLOAD(false, "backup-download", 0),
        BLOGGING_REMINDERS(false, "blogging-reminders", 0),
        CHOOSE_A_LAYOUT(true, "choose-a-layout", 3),
        CREATE_NEW_OPTIONS(true, "create-new-options", 2),
        EDIT_POST(false, "edit-blog-post", 0),
        MEDIA(false, "media", 0),
        MY_SITE(false, "my-site", 0),
        NOTIFICATIONS(true, "notifications", 5),
        SITE_TOPIC(true, "whats-your-website-about", 1),
        SCAN(false, "scan", 0),
        STATS(true, "stats", 4);

        public final boolean enabled;
        public final String screenshotName;
        public final int sequence;

        Screenshots(boolean enabled, String screenshotName, int sequence) {
            this.enabled = enabled;
            this.screenshotName = screenshotName;
            this.sequence = sequence;
        }

        public static String buildScreenshotName(Screenshots screen) {
            return String.format(Locale.US, "%d-%s", screen.sequence, screen.screenshotName);
        }
    }

    @Test
    public void jPScreenshotTest() {
        if (BuildConfig.IS_JETPACK_APP) {
            ActivityScenario.launch(WPLaunchActivity.class);
            Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

            // Enable Demo Mode
            mDemoModeEnabler.enable();
            wpLogin();

            generateActivityLog();
            generateBackupDownload();
            generateBloggingReminders();
            generateBlogPost();
            generateChooseALayout();
            generateCreateNewOptions();
            generateMedia();
            generateMySite();
            generateNotifications();
            generateScan();
            generateSiteTopic();
            generateStats();

            // Turn Demo Mode off on the emulator when we're done
            mDemoModeEnabler.disable();
            logoutIfNecessary();
        }
    }

    // Enabled screenshots
    private void generateCreateNewOptions() {
        if (!Screenshots.CREATE_NEW_OPTIONS.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        (new MySitesPage()).addBloggingPrompts();

        // Navigate back to dashboard
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
        (new MySitesPage()).createPost();

        waitForElementToBeDisplayedWithoutFailure(R.id.recycler_view);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.CREATE_NEW_OPTIONS));

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateChooseALayout() {
        if (!Screenshots.CHOOSE_A_LAYOUT.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Sites" tab in the nav, then click the SiteInfo dropdown
        clickOn(R.id.nav_sites);
        clickOn(R.id.fab_button);

        // Wait for bottom sheet to load
        idleFor(2000);

        // Select Site Page
        clickOn(onView(withText(getTranslatedString(R.string.my_site_bottom_sheet_add_page))));
        idleFor(2000);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.CHOOSE_A_LAYOUT));

        // Exit the view and return
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateNotifications() {
        if (!Screenshots.NOTIFICATIONS.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);

        // Wait for the images to load
        idleFor(6000);

        setNightModeAndWait(false);

        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.NOTIFICATIONS));

        // Exit the notifications activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateSiteTopic() {
        if (!Screenshots.SITE_TOPIC.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Sites" tab in the nav, then click the SiteInfo dropdown
        clickOn(R.id.nav_sites);
        (new MySitesPage()).startNewSite();

        // Wait for page to load
        idleFor(2000);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.SITE_TOPIC));

        // Exit the view and return
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateStats() {
        if (!Screenshots.STATS.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Stats"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToStats().dismissUpdateAlertDialogFragmentIfDisplayed();

        UiTestingUtils.swipeToAvoidGrayOverlayIgnoringFailures(R.id.statsPager);

        if (isElementDisplayed(R.id.button_negative)) {
            clickOn(R.id.button_negative);
        }

        // click on the Month tab
        onView(allOf(withText(R.string.stats_timeframe_months),
                isDescendantOfA(withId(R.id.tabLayout)))).perform(click());

        idleFor(8000);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.STATS));

        // Exit the Stats Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    // Disabled screenshots
    private void generateActivityLog() {
        if (!Screenshots.ACTIVITY_LOG.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Activity Log"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToActivityLog();

        waitForElementToBeDisplayedWithoutFailure(R.id.swipe_refresh_layout);

        // Wait for the activity log to load
        idleFor(8000);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.ACTIVITY_LOG));

        // Exit the Activity Log Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateMySite() {
        if (!Screenshots.MY_SITE.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        waitForElementToBeDisplayedWithoutFailure(R.id.recycler_view);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.MY_SITE));
    }

    private void generateBackupDownload() {
        if (!Screenshots.BACKUP_DOWNLOAD.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToBackup();

        waitForElementToBeDisplayedWithoutFailure(R.id.log_list_view);

        // Wait for backup to load
        idleFor(8000);

        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.action_button), withContentDescription("Activity Log action button"),
                        childAtPosition(
                                allOf(withId(R.id.activity_content_container),
                                        childAtPosition(
                                                withId(R.id.log_list_view),
                                                1)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        DataInteraction linearLayout = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(1);
        linearLayout.perform(click());


        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.BACKUP_DOWNLOAD));

        // Exit the backup download activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateBlogPost() {
        if (!Screenshots.EDIT_POST.enabled) return;

        checkAndNavigateToSiteIfNotShowing("fourpawsdoggrooming.wordpress.com");
        (new MySitesPage()).goToPosts();

        idleFor(3000);

        PostsListPage.goToDrafts();

        // Get a screenshot of the editor with the block library expanded
        // Wait for the editor to load all images
        idleFor(5000);

        screenshotPostWithName("Our Services",
                Screenshots.buildScreenshotName(Screenshots.EDIT_POST),
                false,
                !isTabletScreen());

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateBloggingReminders() {
        if (!Screenshots.BLOGGING_REMINDERS.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Scan"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToBloggingReminders();

        waitForElementToBeDisplayedWithoutFailure(R.id.content_recycler_view);

        // Wait for scan to load
        idleFor(8000);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.BLOGGING_REMINDERS));

        // Exit the Activity scan activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateMedia() {
        if (!Screenshots.MEDIA.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Media"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToMedia();

        waitForElementToBeDisplayedWithoutFailure(R.id.media_browser_container);

        idleFor(2000);
        setNightModeAndWait(true);

        // To do should add the logic for gallery of images
        // Right now on navigating to the media no images will be present in gallery
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.MEDIA));

        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void generateScan() {
        if (!Screenshots.SCAN.enabled) return;

        checkAndNavigateToSiteIfNotShowing();

        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Scan"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToScan();

        waitForElementToBeDisplayedWithoutFailure(R.id.recycler_view);

        // Wait for scan to load
        idleFor(8000);

        setNightModeAndWait(false);
        takeScreenshot(Screenshots.buildScreenshotName(Screenshots.SCAN));

        // Exit the Activity scan activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    // Helper methods
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

        setNightModeAndWait(false);

        if (openBlockList) {
            clickOnViewWithTag("add-block-button");
            idleFor(2000);
        }

        takeScreenshot(screenshotName);
        pressBackUntilElementIsDisplayed(R.id.tabLayout);
    }

    private void setNightModeAndWait(boolean isNightMode) {
        setNightMode(isNightMode);
        idleFor(5000);
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

    private void checkAndNavigateToSiteIfNotShowing() {
        checkAndNavigateToSiteIfNotShowing(JETPACK_SCREENSHOT_SITE_URL);
    }

    private void checkAndNavigateToSiteIfNotShowing(String url) {
        pressBackUntilElementIsDisplayed(R.id.nav_sites);

        if (isTextDisplayed(url)) return;

        (new MySitesPage()).switchToSite(url);
    }
}
