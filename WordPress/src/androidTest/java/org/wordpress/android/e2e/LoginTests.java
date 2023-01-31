package org.wordpress.android.e2e;

import androidx.compose.ui.test.junit4.ComposeTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.support.BaseTest;

import static androidx.compose.ui.test.junit4.AndroidComposeTestRule_androidKt.createComposeRule;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_USERNAME;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_PASSWORDLESS_USER_EMAIL;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_EMAIL;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_SITE_ADDRESS;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class LoginTests extends BaseTest {
    @Rule
    public ComposeTestRule mComposeTestRule = createComposeRule();

    @Before
    public void setUp() {
        logoutIfNecessary();
    }

    @Test
    public void e2eLoginWithEmailPassword() {
        new LoginFlow().chooseContinueWithWpCom(mComposeTestRule)
                       .enterEmailAddress(E2E_WP_COM_USER_EMAIL)
                       .enterPassword(E2E_WP_COM_USER_PASSWORD)
                       .confirmLogin(false);
    }

    @Test
    public void e2eLoginWithPasswordlessAccount() {
        new LoginFlow().chooseContinueWithWpCom(mComposeTestRule)
                       .enterEmailAddress(E2E_WP_COM_PASSWORDLESS_USER_EMAIL)
                       .openMagicLink()
                       .confirmLogin(false);
    }

    @Test
    public void e2eLoginWithSiteAddress() {
        new LoginFlow().chooseEnterYourSiteAddress(mComposeTestRule)
                       .enterSiteAddress(E2E_WP_COM_USER_SITE_ADDRESS)
                       .enterEmailAddress(E2E_WP_COM_USER_EMAIL)
                       .enterPassword(E2E_WP_COM_USER_PASSWORD)
                       .confirmLogin(false);
    }

    @Test
    public void e2eLoginWithMagicLink() {
        new LoginFlow().chooseContinueWithWpCom(mComposeTestRule)
                       .enterEmailAddress(E2E_WP_COM_USER_EMAIL)
                       .chooseMagicLink()
                       .openMagicLink()
                       .confirmLogin(false);
    }

    @Test
    public void e2eLoginWithSelfHostedAccount() {
        new LoginFlow().chooseEnterYourSiteAddress(mComposeTestRule)
                       .enterSiteAddress(E2E_SELF_HOSTED_USER_SITE_ADDRESS)
                       .enterUsernameAndPassword(E2E_SELF_HOSTED_USER_USERNAME, E2E_SELF_HOSTED_USER_PASSWORD)
                       .confirmLogin(true);
    }

    @After
    public void tearDown() {
        logoutIfNecessary();
    }
}
