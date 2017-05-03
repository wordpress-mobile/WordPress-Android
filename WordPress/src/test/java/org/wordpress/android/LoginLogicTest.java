package org.wordpress.android;

import android.os.Build;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.android.ui.accounts.login.nav.LoginNavController;
import org.wordpress.android.ui.accounts.login.nav.LoginNavController.*;
import org.wordpress.android.ui.accounts.login.nav.LoginNav;

/**
 * Testing Login related logic
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "src/main/AndroidManifest.xml", sdk = Build.VERSION_CODES.JELLY_BEAN)
public class LoginLogicTest {

    private String mLastToastMessage;

    private ContextImplementation mContextImplementation = new LoginNavController.ContextImplementation() {
        @Override
        public void toast(String message) {
            mLastToastMessage = message;
        }

        @Override
        public void showEmailLoginScreen() {
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
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mContextImplementation);
        loginNavController.ensureState(LoginNav.Prologue.class);
    }

    @Test
    public void initialStateNotInputEmail() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mContextImplementation);

        exception.expect(RuntimeException.class);
        exception.expectMessage("Not in state " + LoginNav.InputEmail.class.getSimpleName());
        loginNavController.ensureState(LoginNav.InputEmail.class);
    }

    @Test
    public void initialStateNotInputUrl() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class,
                mContextImplementation);

        exception.expect(RuntimeException.class);
        exception.expectMessage("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName());
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);
    }

    /////////////////////////////////////////////////
    ///
    /// PROLOGUE state tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void prologueLoginTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mContextImplementation);

        loginNavController.getLoginNavPrologue().doStartLogin();

        loginNavController.ensureState(LoginNav.InputEmail.class);
    }

    @Test
    public void prologueSignupTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mContextImplementation);

        loginNavController.getLoginNavPrologue().doStartSignup();

        // login is not implemented yet so, we should still be in the prologue state
        loginNavController.ensureState(LoginNav.Prologue.class);
        Assert.assertEquals("Signup is not implemented yet", mLastToastMessage);
    }

    @Test
    public void prologueInvalidEventsTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mContextImplementation);

        {
            // `gotEmail` event is now allowed while in this state
            try {
                loginNavController.getLoginNavInputEmail().gotEmail("a@b.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.InputEmail.class.getSimpleName(), re.getMessage());
            }
        }

        {
            try {
                // `gotSiteAddress` event is now allowed while in this state
                loginNavController.getLoginNavInputSiteAddress().gotSiteAddress("test.wordpress.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName(),
                        re.getMessage());
            }
        }

        // we should still be in the Prologue state
        loginNavController.ensureState(LoginNav.Prologue.class);
    }

    /////////////////////////////////////////////////
    ///
    /// INPUT_EMAIL state tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void inputEmailGotEmailTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputEmail.class, mContextImplementation);
        loginNavController.ensureState(LoginNav.InputEmail.class);

        loginNavController.getLoginNavInputEmail().gotEmail("a@b.com");

        // email input is not implemented yet so, we should still be in the InputEmail state
        loginNavController.ensureState(LoginNav.InputEmail.class);
        Assert.assertEquals("Input email is not implemented yet. Input email: a@b.com", mLastToastMessage);
    }

    @Test
    public void inputEmailFallbackUsernamePasswordTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputEmail.class, mContextImplementation);
        loginNavController.ensureState(LoginNav.InputEmail.class);

        loginNavController.getLoginNavInputEmail().loginViaUsernamePassword();

        // fall back to username/password is not implemented yet so, we should still be in the InputEmail state
        loginNavController.ensureState(LoginNav.InputEmail.class);
        Assert.assertEquals("Fall back to username/password is not implemented yet.", mLastToastMessage);
    }

    @Test
    public void inputEmailInvalidEventsTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputEmail.class, mContextImplementation);

        {
            // `doStartLogin` event is now allowed while in state INPUT_EMAIL
            try {
                loginNavController.getLoginNavPrologue().doStartLogin();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
            }

            // `doStartSignup` event is now allowed while in state INPUT_EMAIL
            try {
                loginNavController.getLoginNavPrologue().doStartSignup();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
            }
        }

        {
            // `gotSiteAddress` event is now allowed while in state INPUT_EMAIL
            try {
                loginNavController.getLoginNavInputSiteAddress().gotSiteAddress("test.wordpress.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName(), re.getMessage());
            }
        }

        // we should still be in the InputEmail state
        loginNavController.ensureState(LoginNav.InputEmail.class);
    }

    /////////////////////////////////////////////////
    ///
    /// INPUT_SITE_ADDRESS state tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void inputSiteAddressGotEmailTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputSiteAddress.class, mContextImplementation);
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);

        loginNavController.getLoginNavInputSiteAddress().gotSiteAddress("test.wordpress.com");

        // site address input is not implemented yet so, we should still be in the InputSiteAddress state
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);
        Assert.assertEquals("Input site address is not implemented yet. Input site address: test.wordpress.com",
                mLastToastMessage);
    }

    @Test
    public void inputSiteAddressInvalidEventsTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputSiteAddress.class, mContextImplementation);

        {

            // `doStartLogin` event is now allowed while in this state
            try {
                loginNavController.getLoginNavPrologue().doStartLogin();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
            }

            // ``doStartSignup` event is now allowed while in this state
            try {
                loginNavController.getLoginNavPrologue().doStartSignup();
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
            }
        }

        {
            // `gotEmail` event is now allowed while in this state
            try {
                loginNavController.getLoginNavInputEmail().gotEmail("a@b.com");
            } catch (RuntimeException re) {
                Assert.assertEquals("Not in state " + LoginNav.InputEmail.class.getSimpleName(), re.getMessage());
            }
        }

        // we should still be in the InputSiteAddress state
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);
    }
}
