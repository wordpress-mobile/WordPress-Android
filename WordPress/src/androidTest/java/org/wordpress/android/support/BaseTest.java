package org.wordpress.android.support;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Before;
import org.junit.Rule;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.modules.AppComponentTest;
import org.wordpress.android.modules.DaggerAppComponentTest;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.util.AssetFileSource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;

public class BaseTest {
    protected WordPress mAppContext;
    protected AppComponentTest mMockedAppComponent;

    private static final String WIREMOCK_PATH = "wiremock";
    public static final int WIREMOCK_PORT = 8080;

    @Before
    public void setup() {
        mAppContext =
                (WordPress) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        mMockedAppComponent = DaggerAppComponentTest.builder()
                                                    .application(mAppContext)
                                                    .build();
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            wireMockConfig().port(WIREMOCK_PORT).fileSource(
                    new AssetFileSource(InstrumentationRegistry.getContext().getAssets(), WIREMOCK_PATH)));
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    private void logout() {
        boolean isSelfHosted = new MePage().go().isSelfHosted();
        if (isSelfHosted) { // Logged in from self hosted connected
            new MySitesPage().go().removeSite(E2E_SELF_HOSTED_USER_SITE_ADDRESS);
        } else {
            wpLogout();
        }
    }

    protected void logoutIfNecessary() {
        if (isElementDisplayed(R.id.login_button) || isElementDisplayed(R.id.login_open_email_client)) {
            return;
        }

        if (isElementDisplayed(R.id.nav_me)) {
            logout();
        }
    }
    protected void wpLogin() {
        new LoginFlow().loginEmailPassword();
    }

    protected void wpLogout() {
        new MePage().go().verifyUsername(E2E_WP_COM_USER_USERNAME).logout();
    }
}
