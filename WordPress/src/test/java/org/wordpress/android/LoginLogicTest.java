package org.wordpress.android;

import android.os.Build;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.android.ui.accounts.login.nav.LoginEvents;
import org.wordpress.android.ui.accounts.login.nav.LoginNavigationController;
import org.wordpress.android.ui.accounts.login.nav.LoginState;

/**
 * Testing Login related logic
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "src/main/AndroidManifest.xml", sdk = Build.VERSION_CODES.JELLY_BEAN)
public class LoginLogicTest {

    private String mLastToastMessage;

    private LoginNavigationController.ContextImplementation mContextImplementation = new LoginNavigationController
            .ContextImplementation() {
        @Override
        public void toast(String message) {
            mLastToastMessage = message;
        }
    };

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /////////////////////////////////////////////////
    ///
    /// Initial state tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void initialStatePrologue() {
        // this shouldn't throw
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.PROLOGUE,
                mContextImplementation);
        loginNavigationController.ensureState(LoginState.PROLOGUE);
    }

    @Test
    public void initialStateNotInputEmail() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.PROLOGUE,
                mContextImplementation);

        exception.expect(RuntimeException.class);
        exception.expectMessage("Not in state " + LoginState.INPUT_EMAIL.name());
        loginNavigationController.ensureState(LoginState.INPUT_EMAIL);
    }

    @Test
    public void initialStateNotInputUrl() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.PROLOGUE,
                mContextImplementation);

        exception.expect(RuntimeException.class);
        exception.expectMessage("Not in state " + LoginState.INPUT_SITE_ADDRESS.name());
        loginNavigationController.ensureState(LoginState.INPUT_SITE_ADDRESS);
    }

    /////////////////////////////////////////////////
    ///
    /// PROLOGUE state tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void prologueLoginTest() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.PROLOGUE,
                mContextImplementation);
        LoginEvents.LoginNavPrologue loginNavPrologue = loginNavigationController.getLoginNavPrologue();
        loginNavPrologue.doStartLogin();

        // login is not implemented yet so, we should still be in the prologue state
        loginNavigationController.ensureState(LoginState.PROLOGUE);
        Assert.assertEquals("Login is not implemented yet", mLastToastMessage);
    }

    @Test
    public void prologueSignupTest() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.PROLOGUE,
                mContextImplementation);
        LoginEvents.LoginNavPrologue loginNavPrologue = loginNavigationController.getLoginNavPrologue();
        loginNavPrologue.doStartSignup();

        // login is not implemented yet so, we should still be in the prologue state
        loginNavigationController.ensureState(LoginState.PROLOGUE);
        Assert.assertEquals("Signup is not implemented yet", mLastToastMessage);
    }

    @Test
    public void prologueInvalidEventsTest() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.PROLOGUE,
                mContextImplementation);

        {
            // `gotEmail` event is now allowed while in state PROLOGUE
            LoginEvents.LoginNavInputEmail loginNavInputEmail = loginNavigationController.getLoginNavInputEmail();

            try {
                loginNavInputEmail.gotEmail("a@b.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.INPUT_EMAIL.name(), re.getMessage());
            }
        }

        {
            // `gotSiteAddress` event is now allowed while in state PROLOGUE
            LoginEvents.LoginNavInputSiteAddress loginNavInputSiteAddress = loginNavigationController
                    .getLoginNavInputSiteAddress();

            try {
                loginNavInputSiteAddress.gotSiteAddress("test.wordpress.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.INPUT_SITE_ADDRESS.name(), re.getMessage());
            }
        }

        // login is not implemented yet so, we should still be in the prologue state
        loginNavigationController.ensureState(LoginState.PROLOGUE);
    }

    /////////////////////////////////////////////////
    ///
    /// INPUT_EMAIL state tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void inputEmailGotEmailTest() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.INPUT_EMAIL,
                mContextImplementation);
        loginNavigationController.ensureState(LoginState.INPUT_EMAIL);

        LoginEvents.LoginNavInputEmail loginNavPrologue = loginNavigationController.getLoginNavInputEmail();
        loginNavPrologue.gotEmail("a@b.com");

        // email input is not implemented yet so, we should still be in the INPUT_EMAIL state
        loginNavigationController.ensureState(LoginState.INPUT_EMAIL);
        Assert.assertEquals("Input email is not implemented yet. Input email: a@b.com", mLastToastMessage);
    }

    @Test
    public void inputEmailInvalidEventsTest() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState.INPUT_EMAIL,
                mContextImplementation);
        loginNavigationController.ensureState(LoginState.INPUT_EMAIL);

        {
            // `doStartLogin` event is now allowed while in state INPUT_EMAIL
            LoginEvents.LoginNavPrologue loginNavPrologue = loginNavigationController.getLoginNavPrologue();

            try {
                loginNavPrologue.doStartLogin();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.PROLOGUE.name(), re.getMessage());
            }
            try {
                loginNavPrologue.doStartSignup();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.PROLOGUE.name(), re.getMessage());
            }
        }

        {
            // `gotSiteAddress` event is now allowed while in state INPUT_EMAIL
            LoginEvents.LoginNavInputSiteAddress loginNavInputSiteAddress = loginNavigationController
                    .getLoginNavInputSiteAddress();

            try {
                loginNavInputSiteAddress.gotSiteAddress("test.wordpress.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.INPUT_SITE_ADDRESS.name(), re.getMessage());
            }
        }

        // login is not implemented yet so, we should still be in the INPUT_EMAIL state
        loginNavigationController.ensureState(LoginState.INPUT_EMAIL);
    }

    /////////////////////////////////////////////////
    ///
    /// INPUT_SITE_ADDRESS state tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void inputSiteAddressGotEmailTest() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState
                .INPUT_SITE_ADDRESS, mContextImplementation);
        loginNavigationController.ensureState(LoginState.INPUT_SITE_ADDRESS);

        LoginEvents.LoginNavInputSiteAddress loginNavInputSiteAddress = loginNavigationController
                .getLoginNavInputSiteAddress();
        loginNavInputSiteAddress.gotSiteAddress("test.wordpress.com");

        // site address input is not implemented yet so, we should still be in the INPUT_SITE_ADDRESS state
        loginNavigationController.ensureState(LoginState.INPUT_SITE_ADDRESS);
        Assert.assertEquals("Input site address is not implemented yet. Input site address: test.wordpress.com",
                mLastToastMessage);
    }

    @Test
    public void inputSiteAddressInvalidEventsTest() {
        LoginNavigationController loginNavigationController = new LoginNavigationController(LoginState
                .INPUT_SITE_ADDRESS, mContextImplementation);
        loginNavigationController.ensureState(LoginState.INPUT_SITE_ADDRESS);

        {
            // `doStartLogin` and `doStartSignup` events is now allowed while in state INPUT_EMAIL
            LoginEvents.LoginNavPrologue loginNavPrologue = loginNavigationController.getLoginNavPrologue();

            try {
                loginNavPrologue.doStartLogin();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.PROLOGUE.name(), re.getMessage());
            }
            try {
                loginNavPrologue.doStartSignup();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.PROLOGUE.name(), re.getMessage());
            }
        }

        {
            // `gotEmail` event is now allowed while in state INPUT_SITE_ADDRESS
            LoginEvents.LoginNavInputEmail loginNavInputEmail = loginNavigationController.getLoginNavInputEmail();

            try {
                loginNavInputEmail.gotEmail("a@b.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginState.INPUT_EMAIL.name(), re.getMessage());
            }
        }

        // login is not implemented yet so, we should still be in the INPUT_EMAIL state
        loginNavigationController.ensureState(LoginState.INPUT_SITE_ADDRESS);
    }
}