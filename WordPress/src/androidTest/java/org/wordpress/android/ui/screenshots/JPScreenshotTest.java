package org.wordpress.android.ui.screenshots;

import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;

import com.google.android.libraries.cloudtesting.screenshots.ScreenShotter;

import org.junit.ClassRule;
import org.junit.Test;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.support.DemoModeEnabler;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.util.image.ImageType;

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
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.setNightMode;
import static org.wordpress.android.support.WPSupportUtils.swipeLeftOnViewPager;
import static org.wordpress.android.support.WPSupportUtils.swipeRightOnViewPager;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;

import dagger.hilt.android.testing.HiltAndroidTest;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

@LargeTest
@HiltAndroidTest
public class JPScreenshotTest extends BaseTest {
    @ClassRule
    public static final WPLocaleTestRule LOCALE_TEST_RULE = new WPLocaleTestRule();

    private DemoModeEnabler mDemoModeEnabler = new DemoModeEnabler();

    private static final String MY_SITE_SCREENSHOT_NAME = "1-bring-your-jetpack-with-you";
    private static final String ACTIVITY_LOG_SCREENSHOT_NAME = "2-keep-tabs-on-your-site-activity";
    private static final String SCAN_SCREENSHOT_NAME = "3-scan-for-issues-on-the-go";
    private static final String BACKUP_SCREENSHOT_NAME = "4-back-up-your-site-at-any-moment";
    private static final String STATS_SCREENSHOT_NAME = "5-site-stats-in-your-pocket";
    private static final String NOTIFICATIONS_SCREENSHOT_NAME = "6-reply-in-real-time";

    @Test
    public void jPScreenshotTest() {
        if (BuildConfig.IS_JETPACK_APP) {
            ActivityScenario.launch(WPLaunchActivity.class);
            Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

            // Enable Demo Mode
            mDemoModeEnabler.enable();
            wpLogin();

            navigateMySite();
            navigateActivityLog();
//            navigateScan();
//            navigateBackupDownload();
            navigateStats();
            navigateNotifications();

            // Turn Demo Mode off on the emulator when we're done
            mDemoModeEnabler.disable();
            logoutIfNecessary();
        }
    }

    public void navigateMySite() {
        (new MySitesPage()).switchToSite("yourjetpack.blog");

        waitForElementToBeDisplayedWithoutFailure(R.id.recycler_view);

        setNightModeAndWait(false);
        takeScreenshot(MY_SITE_SCREENSHOT_NAME);
    }

    private void navigateActivityLog() {
        moveToActivityLog();

        setNightModeAndWait(false);
        takeScreenshot(ACTIVITY_LOG_SCREENSHOT_NAME);

        // Exit the Activity Log Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateScan() {
        moveToScan();

        setNightModeAndWait(false);
        takeScreenshot(SCAN_SCREENSHOT_NAME);

        // Exit the Activity scan activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateBackupDownload() {
        moveToBackup();

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
        takeScreenshot(BACKUP_SCREENSHOT_NAME);

        // Exit the backup download activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateStats() {
        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Stats"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToStats().dismissUpdateAlertDialogFragmentIfDisplayed();

        swipeToAvoidGrayOverlay(R.id.statsPager);

        if (isElementDisplayed(R.id.button_negative)) {
            clickOn(R.id.button_negative);
        }

        // click on the Month tab
        onView(allOf(withText(R.string.stats_timeframe_months),
                isDescendantOfA(withId(R.id.tabLayout)))).perform(click());

        idleFor(8000);

        setNightModeAndWait(false);
        takeScreenshot(STATS_SCREENSHOT_NAME);

        // Exit the Stats Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateNotifications() {
        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);

        // Wait for the images to load
        idleFor(6000);

        setNightModeAndWait(false);

        takeScreenshot(NOTIFICATIONS_SCREENSHOT_NAME);

        // Exit the notifications activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }
    private void moveToActivityLog() {
        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Activity Log"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToActivityLog();

        waitForElementToBeDisplayedWithoutFailure(R.id.swipe_refresh_layout);

        // Wait for the activity log to load
        idleFor(8000);
    }

    private void moveToScan() {
        // Click on the "Sites" tab in the nav, then click the "Menu" tab, then choose "Scan"
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToScan();

        waitForElementToBeDisplayedWithoutFailure(R.id.recycler_view);

        // Wait for scan to load
        idleFor(8000);
    }

    private void moveToBackup() {
        clickOn(R.id.nav_sites);
        (new MySitesPage()).goToBackup();

        waitForElementToBeDisplayedWithoutFailure(R.id.log_list_view);

        // Wait for backup to load
        idleFor(8000);
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

    // In some cases there's a gray overlay on view pager screens when taking screenshots
    // this function swipes left and then right as a workaround to clear it
    // resourceID should be the ID of the viewPager
    private void swipeToAvoidGrayOverlay(int resourceID) {
        // Workaround to avoid gray overlay
        swipeLeftOnViewPager(resourceID);
        idleFor(1000);
        swipeRightOnViewPager(resourceID);
        idleFor(1000);
    }

    private void setNightModeAndWait(boolean isNightMode) {
        setNightMode(isNightMode);
        idleFor(5000);
    }
}
