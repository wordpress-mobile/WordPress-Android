package org.wordpress.android.e2e;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.support.BaseTest;

import static org.wordpress.android.support.WPSupportUtils.sleep;

@RunWith(AndroidJUnit4.class)
public class LoginTests extends BaseTest {
    @Before
    public void setUp() {
        logoutIfNecessary();
    }

    @Test
    public void loginWithEmailPassword() {
        wpLogin();

        // Wait for login to finish loading
        sleep();
    }

    @Test
    public void loginWithMagicLink() {
        new LoginFlow().loginMagicLink();
    }

    @Test
    public void loginWithSelfHostedAccount() {
        new LoginFlow().loginSiteAddress();
    }

    @After
    public void tearDown() {
        logout();
    }
}
