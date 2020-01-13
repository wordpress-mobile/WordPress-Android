package org.wordpress.android.support;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

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
    public WireMockRule wireMockRule = new WireMockRule(
            options().port(WIREMOCK_PORT)
                     .fileSource(new AssetFileSource(
                             InstrumentationRegistry.getInstrumentation().getContext().getAssets()))
                     .extensions(new ResponseTemplateTransformer(true))
                     .notifier(new AndroidNotifier()));

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
        new LoginFlow().loginEmailPassword();
    }

    private void wpLogout() {
        new MePage().go().logout();
    }
}
