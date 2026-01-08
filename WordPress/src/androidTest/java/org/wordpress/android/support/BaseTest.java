package org.wordpress.android.support;


import androidx.annotation.NonNull;
import androidx.compose.ui.test.junit4.ComposeTestRule;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult;
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType;
import com.google.gson.internal.bind.util.ISO8601Utils;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.InitializationRule;
import org.wordpress.android.R;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.rules.RetryTestRule;
import org.wordpress.android.ui.WPLaunchActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import dagger.hilt.android.testing.HiltAndroidRule;

import static androidx.compose.ui.test.junit4.AndroidComposeTestRule_androidKt.createComposeRule;
import static com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesTypes;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.wordpress.android.support.E2ECredentials.SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.support.E2ECredentials.WP_COM_USER_EMAIL;
import static org.wordpress.android.support.E2ECredentials.WP_COM_USER_PASSWORD;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;

public class BaseTest {
    static final String TAG = BaseTest.class.getSimpleName();

    @Rule(order = 0)
    public HiltAndroidRule mHiltRule = new HiltAndroidRule(this);

    @Rule(order = 1)
    public InitializationRule mInitializationRule = new InitializationRule();

    @Rule(order = 2)
    public ComposeTestRule mComposeTestRule = createComposeRule();

    @Rule(order = 3)
    public ActivityScenarioRule<WPLaunchActivity> mActivityScenarioRule
            = new ActivityScenarioRule<>(WPLaunchActivity.class);

    @Rule(order = 4)
    public RetryTestRule retryTestRule = new RetryTestRule();

    @Before
    public void setup() {
        Matcher<? super AccessibilityCheckResult> nonErrorLevelMatcher =
                Matchers.allOf(matchesTypes(
                        anyOf(is(AccessibilityCheckResultType.INFO), is(AccessibilityCheckResultType.WARNING))));
        AccessibilityChecks.enable().setRunChecksFromRootView(true).setThrowExceptionForErrors(false)
                           .setSuppressingResultMatcher(nonErrorLevelMatcher);

        disableAutoSyncWithComposeUiInJetpackApp();
    }

    /**
     * Disable auto-sync with Compose UI.
     * @see <a href="https://developer.android.com/jetpack/compose/testing#disable-autosync">Disabling Auto Sync</a>
     */
    private void disableAutoSyncWithComposeUiInJetpackApp() {
        if (BuildConfig.IS_JETPACK_APP) {
            mComposeTestRule.getMainClock().setAutoAdvance(false);
        }
    }

    private void logout() {
        MePage mePage = new MePage();
        boolean isSelfHosted = mePage.go().isSelfHosted();
        if (isSelfHosted) { // Logged in from self hosted connected
            new MySitesPage().go().removeSite(SELF_HOSTED_USER_SITE_ADDRESS);
        } else {
            wpLogout();
        }
    }

    protected void logoutIfNecessary() {
        if (isElementDisplayed(R.id.nav_sites)) {
            logout();
        }
    }

    protected void wpLogin() {
        logoutIfNecessary();
        new LoginFlow().chooseContinueWithWpCom(mComposeTestRule)
                       .enterEmailAddress(WP_COM_USER_EMAIL)
                       .enterPassword(WP_COM_USER_PASSWORD)
                       .confirmLogin();
    }

    private void wpLogout() {
        new MePage().go().logout();
    }
}

class LocaleAwareRenderableDate {
    private static final long DIVIDE_MILLISECONDS_TO_SECONDS = 1000L;

    private final Date mDate;
    private final String mFormat;
    private final String mTimezoneName;
    private final Locale mLocale;

    LocaleAwareRenderableDate(Date date, String format, String timezone, Locale locale) {
        this.mDate = date;
        this.mFormat = format;
        this.mTimezoneName = timezone;
        this.mLocale = locale;
    }

    @NonNull @Override
    public String toString() {
        if (mFormat != null) {
            if (mFormat.equals("epoch")) {
                return String.valueOf(mDate.getTime());
            }

            if (mFormat.equals("unix")) {
                return String.valueOf(mDate.getTime() / DIVIDE_MILLISECONDS_TO_SECONDS);
            }

            return formatCustom();
        }

        return mTimezoneName != null
                ? ISO8601Utils.format(mDate, false, TimeZone.getTimeZone(mTimezoneName))
                : ISO8601Utils.format(mDate, false);
    }

    private String formatCustom() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(mFormat, mLocale);
        if (mTimezoneName != null) {
            TimeZone zone = TimeZone.getTimeZone(mTimezoneName);
            dateFormat.setTimeZone(zone);
        }
        return dateFormat.format(mDate);
    }
}
