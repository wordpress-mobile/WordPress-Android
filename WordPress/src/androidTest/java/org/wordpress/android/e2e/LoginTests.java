package org.wordpress.android.e2e;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.support.BaseTest;

import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;

@RunWith(AndroidJUnit4.class)
public class LoginTests extends BaseTest {
    @Test
    public void testWPComLoginLogout() {
        wpLogin();
        sleep();

        new MePage().go().verifyUsername(E2E_WP_COM_USER_USERNAME).logout();
    }
}
