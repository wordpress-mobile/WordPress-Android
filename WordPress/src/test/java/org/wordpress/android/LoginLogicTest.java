package org.wordpress.android;

import android.os.Build;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.android.ui.accounts.login.nav.LoginNavHandler;
import org.wordpress.android.ui.accounts.login.nav.LoginNav;
import org.wordpress.android.ui.accounts.login.nav.LoginNavController;

/**
 * Testing Login related logic
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "src/main/AndroidManifest.xml", sdk = Build.VERSION_CODES.JELLY_BEAN)
public class LoginLogicTest {

    private String mLastToastMessage;

    private LoginNavHandler mLoginNavHandler = new LoginNavHandler() {
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
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mLoginNavHandler);
        loginNavController.ensureState(LoginNav.Prologue.class);
    }

    @Test
    public void initialStateNotInputEmail() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mLoginNavHandler);

        exception.expect(RuntimeException.class);
        exception.expectMessage("Not in state " + LoginNav.InputEmail.class.getSimpleName());
        loginNavController.ensureState(LoginNav.InputEmail.class);
    }

    @Test
    public void initialStateNotInputUrl() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mLoginNavHandler);

        exception.expect(RuntimeException.class);
        exception.expectMessage("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName());
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);
    }

    /////////////////////////////////////////////////
    ///
    /// PROLOGUE nav tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void prologueLoginTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mLoginNavHandler);

        loginNavController.getLoginNavPrologue().doStartLogin();

        loginNavController.ensureState(LoginNav.InputEmail.class);

        loginNavController.goBack();
        loginNavController.ensureState(LoginNav.Prologue.class);
    }

    @Test
    public void prologueSignupTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mLoginNavHandler);

        loginNavController.getLoginNavPrologue().doStartSignup();

        // login is not implemented yet so, we should still be in the prologue state
        loginNavController.ensureState(LoginNav.Prologue.class);
        Assert.assertEquals("Signup is not implemented yet", mLastToastMessage);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }

    @Test
    public void prologueInvalidEventsTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputEmail.class, mLoginNavHandler);

        // get a reference to LoginNav InputEmail for later use
        LoginNav.InputEmail loginNavInputEmail = loginNavController.getLoginNavInputEmail();

        // force LoginNav InputSiteAddress
        loginNavController.goBack();
        loginNavController.force(LoginNav.InputSiteAddress.class);
        // get a reference to LoginNav InputSiteAddress for later use
        LoginNav.InputSiteAddress loginNavInputSiteAddress = loginNavController.getLoginNavInputSiteAddress();

        // force the state we want to test
        loginNavController.goBack();
        loginNavController.force(LoginNav.Prologue.class);
        loginNavController.ensureState(LoginNav.Prologue.class);

        // we shouldn't be able to obtain a reference to LoginNav InputEmail in this state
        try {
            loginNavInputEmail = loginNavController.getLoginNavInputEmail();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputEmail.class.getSimpleName(), re.getMessage());
        }

        // `gotEmail` event is now allowed while in this state
        try {
            loginNavInputEmail.gotEmail("a@b.com");
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputEmail.class.getSimpleName(), re.getMessage());
        }

        // we shouldn't be able to obtain a reference to LoginNav InputSiteAddress in this state
        try {
            loginNavInputSiteAddress = loginNavController.getLoginNavInputSiteAddress();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName(), re.getMessage());
        }

        // `gotSiteAddress` event is now allowed while in this state
        try {
            loginNavInputSiteAddress.gotSiteAddress("test.wordpress.com");
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName(), re.getMessage());
        }

        // we should still be in the Prologue state
        loginNavController.ensureState(LoginNav.Prologue.class);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }

    /////////////////////////////////////////////////
    ///
    /// INPUT_EMAIL nav tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void inputEmailGotEmailTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputEmail.class, mLoginNavHandler);
        loginNavController.ensureState(LoginNav.InputEmail.class);

        loginNavController.getLoginNavInputEmail().gotEmail("a@b.com");

        // email input is not implemented yet so, we should still be in the InputEmail state
        loginNavController.ensureState(LoginNav.InputEmail.class);
        Assert.assertEquals("Input email is not implemented yet. Input email: a@b.com", mLastToastMessage);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }

    @Test
    public void inputEmailFallbackUsernamePasswordTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputEmail.class, mLoginNavHandler);
        loginNavController.ensureState(LoginNav.InputEmail.class);

        loginNavController.getLoginNavInputEmail().loginViaUsernamePassword();

        // fall back to username/password is not implemented yet so, we should still be in the InputEmail state
        loginNavController.ensureState(LoginNav.InputEmail.class);
        Assert.assertEquals("Fall back to username/password is not implemented yet.", mLastToastMessage);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }

    @Test
    public void inputEmailHelpTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputEmail.class, mLoginNavHandler);
        loginNavController.ensureState(LoginNav.InputEmail.class);

        loginNavController.getLoginNavInputEmail().help();

        // help is not implemented yet so, we should still be in the InputEmail state
        loginNavController.ensureState(LoginNav.InputEmail.class);
        Assert.assertEquals("Help is not implemented yet.", mLastToastMessage);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }

    @Test
    public void inputEmailInvalidEventsTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mLoginNavHandler);

        // get a reference to LoginNav Prologue for later use
        LoginNav.Prologue loginNavPrologue = loginNavController.getLoginNavPrologue();

        // force LoginNav InputSiteAddress state
        loginNavController.goBack();
        loginNavController.force(LoginNav.InputSiteAddress.class);
        // get a reference to LoginNav InputSiteAddress for later use
        LoginNav.InputSiteAddress loginNavInputSiteAddress = loginNavController.getLoginNavInputSiteAddress();

        // force the state we want to test
        loginNavController.goBack();
        loginNavController.force(LoginNav.InputEmail.class);
        loginNavController.ensureState(LoginNav.InputEmail.class);

        // we shouldn't be able to obtain a reference to LoginNav Prologue in this state
        try {
            loginNavPrologue = loginNavController.getLoginNavPrologue();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
        }

        // `doStartLogin` and `doStartSignup` events is now allowed while in this state
        try {
            loginNavPrologue.doStartLogin();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
        }
        try {
            loginNavPrologue.doStartSignup();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
        }

        // we shouldn't be able to obtain a reference to LoginNav InputSiteAddress in this state
        try {
            loginNavInputSiteAddress = loginNavController.getLoginNavInputSiteAddress();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName(), re.getMessage());
        }

        // `gotSiteAddress` event is now allowed while in this state
        try {
            loginNavInputSiteAddress.gotSiteAddress("test.wordpress.com");
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName(), re.getMessage());
        }

        // we should still be in the InputEmail state
        loginNavController.ensureState(LoginNav.InputEmail.class);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }

    /////////////////////////////////////////////////
    ///
    /// INPUT_SITE_ADDRESS nav tests
    ///
    /////////////////////////////////////////////////

    @Test
    public void inputSiteAddressGotEmailTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.InputSiteAddress.class, mLoginNavHandler);
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);

        loginNavController.getLoginNavInputSiteAddress().gotSiteAddress("test.wordpress.com");

        // site address input is not implemented yet so, we should still be in the InputSiteAddress state
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);
        Assert.assertEquals("Input site address is not implemented yet. Input site address: test.wordpress.com",
                mLastToastMessage);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }

    @Test
    public void inputSiteAddressInvalidEventsTest() {
        LoginNavController loginNavController = new LoginNavController(LoginNav.Prologue.class, mLoginNavHandler);

        // get a reference to LoginNav Prologue for later use
        LoginNav.Prologue loginNavPrologue = loginNavController.getLoginNavPrologue();

        // force LoginNav InputEmail state
        loginNavController.goBack();
        loginNavController.force(LoginNav.InputEmail.class);
        // get a reference to LoginNav InputEmail for later use
        LoginNav.InputEmail loginNavInputEmail = loginNavController.getLoginNavInputEmail();

        // force the state we want to test
        loginNavController.goBack();
        loginNavController.force(LoginNav.InputSiteAddress.class);
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);

        // we shouldn't be able to obtain a reference to LoginNav Prologue in this state
        try {
            loginNavPrologue = loginNavController.getLoginNavPrologue();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
        }

        // `doStartLogin` and `doStartSignup` events is now allowed while in this state
        try {
            loginNavPrologue.doStartLogin();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
        }
        try {
            loginNavPrologue.doStartSignup();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.Prologue.class.getSimpleName(), re.getMessage());
        }

        // we shouldn't be able to obtain a reference to LoginNav InputEmail in this state
        try {
            loginNavInputEmail = loginNavController.getLoginNavInputEmail();
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputEmail.class.getSimpleName(), re.getMessage());
        }

        // `gotEmail` event is now allowed while in this state
        try {
            loginNavInputEmail.gotEmail("a@b.com");
        } catch (RuntimeException re) {
            Assert.assertEquals("Not in state " + LoginNav.InputEmail.class.getSimpleName(), re.getMessage());
        }

        // we should still be in the InputSiteAddress state
        loginNavController.ensureState(LoginNav.InputSiteAddress.class);

        loginNavController.goBack();
        Assert.assertTrue(loginNavController.isNavStackEmpty());
    }
}
