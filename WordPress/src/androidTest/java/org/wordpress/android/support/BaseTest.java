package org.wordpress.android.support;

import android.app.Instrumentation;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.compose.ui.test.junit4.ComposeTestRule;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.DateOffset;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.HandlebarsHelper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult;
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType;

import org.apache.commons.lang3.LocaleUtils;
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
import org.wordpress.android.editor.Utils;
import org.wordpress.android.mocks.AndroidNotifier;
import org.wordpress.android.mocks.AssetFileSource;
import org.wordpress.android.rules.RetryTestRule;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.wiremock.WireMockStub;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import dagger.hilt.android.testing.HiltAndroidRule;

import static androidx.compose.ui.test.junit4.AndroidComposeTestRule_androidKt.createComposeRule;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesTypes;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_EMAIL;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_PASSWORD;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;

public class BaseTest {
    static final String TAG = BaseTest.class.getSimpleName();
    public static final int WIREMOCK_PORT = 8080;

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
    public WireMockRule wireMockRule;

    @Rule(order = 5)
    public RetryTestRule retryTestRule = new RetryTestRule();

    public BaseTest() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param wireMockStubs the wiremock stubs to use for this specific test.
     */
    public BaseTest(@Nullable final List<WireMockStub> wireMockStubs) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        wireMockRule = new WireMockRule(
                options().port(WIREMOCK_PORT)
                         .fileSource(
                             new AssetFileSource(instrumentation.getContext().getAssets())
                         )

                         .extensions(new ResponseTemplateTransformer(true, new HashMap<String, Helper>() {
                             {
                                 put("fnow", new UnlocalizedDateHelper());
                             }
                         }))
                         .notifier(new AndroidNotifier()));
        if (wireMockStubs != null && !wireMockStubs.isEmpty()) {
            for (WireMockStub wireMockStub : wireMockStubs) {
                try {
                    final String result = Utils.getStringFromInputStream(
                            instrumentation.getContext().getClassLoader().getResourceAsStream(
                                    wireMockStub.getFileName()
                            )
                    );
                    // This is where we can stub out
                    wireMockRule.stubFor(WireMock.get(WireMock.urlPathMatching(wireMockStub.getUrlPath().getPath()))
                                                 .willReturn(WireMock.aResponse().withBody(result)));
                } catch (final Exception exception) {
                    Log.e(TAG, "Problem stubbing endpoint", exception);
                }
            }
        }
    }

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
            new MySitesPage().go().removeSite(E2E_SELF_HOSTED_USER_SITE_ADDRESS);
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
                       .enterEmailAddress(E2E_WP_COM_USER_EMAIL)
                       .enterPassword(E2E_WP_COM_USER_PASSWORD)
                       .confirmLogin();
    }

    private void wpLogout() {
        new MePage().go().logout();
    }
}

class UnlocalizedDateHelper extends HandlebarsHelper<Object> {
    @Override public Object apply(Object context, Options options) throws IOException {
        String format = options.hash("format", null);
        String offset = options.hash("offset", null);
        String timezone = options.hash("timezone", null);
        String localeCode = options.hash("locale", "en_US_POSIX");

        Date date = new Date();
        if (offset != null) {
            date = new DateOffset(offset).shift(date);
        }

        Locale locale = Locale.getDefault();
        if (localeCode != null) {
            locale = LocaleUtils.toLocale(localeCode);
        }

        return new LocaleAwareRenderableDate(date, format, timezone, locale);
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

    @Override
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
