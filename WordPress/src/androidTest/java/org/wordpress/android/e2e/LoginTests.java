package org.wordpress.android.e2e;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.support.BaseTest;

import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_USERNAME;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_SITE_ADDRESS;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;

@RunWith(AndroidJUnit4.class)
public class LoginTests extends BaseTest {
    @Before
    public void setUp() {
        logoutIfNecessary();
    }

    @Test
    public void loginWithEmailPassword() {
        wpLogin();
    }

    @Test
    public void loginWithSiteAddress() {
        new LoginFlow().loginSiteAddress(
                E2E_WP_COM_USER_SITE_ADDRESS,
                E2E_WP_COM_USER_USERNAME,
                E2E_WP_COM_USER_PASSWORD);
    }

    @Test
    public void loginWithMagicLink() {
        new LoginFlow().loginMagicLink();
    }

    @Test
    public void loginWithSelfHostedAccount() {
        new LoginFlow().loginSiteAddress(
                E2E_SELF_HOSTED_USER_SITE_ADDRESS,
                E2E_SELF_HOSTED_USER_USERNAME,
                E2E_SELF_HOSTED_USER_PASSWORD);
    }

    @After
    public void tearDown() {
        logoutIfNecessary();
    }
}
