package org.wordpress.android.ui.screenshots;

import android.provider.Settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.google.android.libraries.cloudtesting.screenshots.ScreenShotter;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.SitePickerPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.support.DemoModeEnabler;
import org.wordpress.android.ui.WPLaunchActivity;

import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.setNightMode;
import static org.wordpress.android.support.WPSupportUtils.swipeLeftOnViewPager;
import static org.wordpress.android.support.WPSupportUtils.swipeRightOnViewPager;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class JPScreenshotTest extends BaseTest {
    @ClassRule
    public static final WPLocaleTestRule LOCALE_TEST_RULE = new WPLocaleTestRule();

    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class,
            false, false);

    private DemoModeEnabler mDemoModeEnabler = new DemoModeEnabler();

    @Test
    public void jPScreenshotTest() {
        if (BuildConfig.IS_JETPACK_APP) {
            mActivityTestRule.launchActivity(null);
            Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

            // Enable Demo Mode
            mDemoModeEnabler.enable();
            wpLogin();

            navigateMySite();

            // Turn Demo Mode off on the emulator when we're done
            mDemoModeEnabler.disable();
            logoutIfNecessary();
        }
    }

    public void navigateMySite() {
        // Click on the "Sites" tab and take a screenshot
        clickOn(R.id.nav_sites);

        // Choose "Switch Site"
        clickOn(R.id.switch_site);

        (new SitePickerPage()).chooseSiteWithURL("yourjetpack.blog");
        waitForElementToBeDisplayedWithoutFailure(R.id.row_blog_posts);

        setNightModeAndWait(false);
        takeScreenshot("1-bring-your-jetpack-with-you");
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
