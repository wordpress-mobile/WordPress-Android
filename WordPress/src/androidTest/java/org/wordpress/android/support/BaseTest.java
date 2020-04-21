package org.wordpress.android.support;

import android.app.Instrumentation;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.DateOffset;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.HandlebarsHelper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.commons.lang3.LocaleUtils;
import org.junit.Before;
import org.junit.Rule;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.mocks.AndroidNotifier;
import org.wordpress.android.mocks.AssetFileSource;
import org.wordpress.android.modules.AppComponentTest;
import org.wordpress.android.modules.DaggerAppComponentTest;
import org.wordpress.android.ui.WPLaunchActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;

public class BaseTest {
    protected WordPress mAppContext;
    protected AppComponentTest mMockedAppComponent;

    public static final int WIREMOCK_PORT = 8080;

    @Before
    public void setup() {
        mAppContext = ApplicationProvider.getApplicationContext();
        mMockedAppComponent = DaggerAppComponentTest.builder()
                                                    .application(mAppContext)
                                                    .build();
    }

    @Rule
    public WireMockRule wireMockRule;

    {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        wireMockRule = new WireMockRule(
                options().port(WIREMOCK_PORT)
                         .fileSource(new AssetFileSource(instrumentation.getContext().getAssets()))
                         .extensions(new ResponseTemplateTransformer(true, new HashMap<String, Helper>() {
                             {
                                 put("fnow", new UnlocalizedDateHelper());
                             }
                         }))
                         .notifier(new AndroidNotifier()));
    }

    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    private void logout() {
        MePage mePage = new MePage();
        boolean isSelfHosted = mePage.go().isSelfHosted();
        if (isSelfHosted) { // Logged in from self hosted connected
            mePage.goBack();
            new MySitesPage().go().removeSite(E2E_SELF_HOSTED_USER_SITE_ADDRESS);
        } else {
            wpLogout();
        }
    }

    protected void logoutIfNecessary() {
        if (isElementDisplayed(R.id.login_button) || isElementDisplayed(R.id.login_open_email_client)) {
            return;
        }

        if (isElementDisplayed(R.id.nav_sites)) {
            logout();
        }
    }
    protected void wpLogin() {
        logoutIfNecessary();
        new LoginFlow().chooseLogin()
                       .enterEmailAddress()
                       .enterPassword()
                       .confirmLogin();
    }

    private void wpLogout() {
        new MePage().go().logout();
    }
}

class UnlocalizedDateHelper extends HandlebarsHelper<Date> {
    @Override public Object apply(Date context, Options options) throws IOException {
        String format = options.hash("format", null);
        String offset = options.hash("offset", null);
        String timezone = options.hash("timezone", null);
        String localeCode = options.hash("locale", "US");

        Date date = context != null ? context : new Date();
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
