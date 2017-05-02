package org.wordpress.android;

import android.os.Build;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.android.ui.accounts.login.nav.LoginNavigationFsm;
import org.wordpress.android.ui.accounts.login.nav.LoginState;
import org.wordpress.android.ui.accounts.login.nav.LoginStateHandler;

/**
 * Testing Login related logic
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        manifest = "src/main/AndroidManifest.xml",
        sdk = Build.VERSION_CODES.JELLY_BEAN)
public class LoginLogicTest {

    private LoginNavigationFsm mLoginNavigationFsm;
    private String mLastToastMessage;

    /**
     * Initialize variables.
     */
    @Before
    public void init() {
        mLoginNavigationFsm = new LoginNavigationFsm(new LoginNavigationFsm.ContextImplementation() {
            @Override
            public void toast(String message) {
                mLastToastMessage = message;
            }
        });
        mLoginNavigationFsm.register();

        Assert.assertEquals(LoginState.PROLOGUE, mLoginNavigationFsm.getCurrentLoginState());
    }

    @After
    public void deinit() {
        mLoginNavigationFsm.unregister();
    }

    @Test()
    public void prologueLoginTest() {
        LoginStateHandler.ActionLogin.emit();

        // login is not implemented yet so, we should still be in the prologue state
        Assert.assertEquals(LoginState.PROLOGUE, mLoginNavigationFsm.getCurrentLoginState());
        Assert.assertEquals("Login is not implemented yet", mLastToastMessage);
    }

    @Test()
    public void prologueSignupTest() {
        LoginStateHandler.ActionSignup.emit();

        // login is not implemented yet so, we should still be in the prologue state
        Assert.assertEquals(LoginState.PROLOGUE, mLoginNavigationFsm.getCurrentLoginState());
        Assert.assertEquals("Signup is not implemented yet", mLastToastMessage);
    }
}