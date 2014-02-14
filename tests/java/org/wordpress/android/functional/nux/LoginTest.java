package org.wordpress.android.functional.nux;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.RenamingDelegatingContext;

import com.robotium.solo.Solo;

import org.wordpress.android.TestUtils;
import org.wordpress.android.functional.FactoryUtils;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.ui.accounts.WelcomeActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class LoginTest extends ActivityInstrumentationTestCase2<WelcomeActivity> {
    private Solo mSolo;
    private Context mTargetContext;

    public LoginTest() {
        super(WelcomeActivity.class);
        FactoryUtils.initWithTestFactories();
    }

    @Override
    public void setUp() throws Exception {
        // Clean application state
        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        TestUtils.clearApplicationState(mTargetContext);

        // setUp() is run before a test case is started.
        // This is where the mSolo object is created.
        mSolo = new Solo(getInstrumentation(), getActivity());

        // Init contexts
        XMLRPCFactoryTest.sContext = getInstrumentation().getContext();
        RestClientFactoryTest.sContext = getInstrumentation().getContext();
        AppLog.v(T.TESTS, "Contexts set");

        // Set mode to Customizable
        XMLRPCFactoryTest.sMode = XMLRPCFactoryTest.Mode.CUSTOMIZABLE;
        RestClientFactoryTest.sMode = RestClientFactoryTest.Mode.CUSTOMIZABLE;
        AppLog.v(T.TESTS, "Modes set to customizable");
    }

    @Override
    public void tearDown() throws Exception {
        // tearDown() is run after a test case has finished.
        // finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        mSolo.finishOpenedActivities();
    }

    public void assertLoggedIn() {
        mSolo.waitForActivity("WPActionBar");
        assertTrue(true);
    }

    public void testGoodCredentials() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText("Sign in");
        assertLoggedIn();
    }

    public void testBadCredentials() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("login-failure");
        XMLRPCFactoryTest.setPrefixAllInstances("login-failure");
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText("Sign in");
        boolean errorMessageFound = mSolo.searchText("username or password");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountInvalidEmail() throws Exception {
        mSolo.clickOnText("Create account");
        mSolo.waitForText(".*Create an account.*");
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText("Create account");
        boolean errorMessageFound = mSolo.searchText("address isn't valid");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountUsernameExists() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("username-exists");
        mSolo.clickOnText("Create account");
        mSolo.waitForText(".*Create an account.*");
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText("Create account");
        boolean errorMessageFound = mSolo.searchText("username already exists");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountPasswordTooShort() throws Exception {
        mSolo.clickOnText("Create account");
        mSolo.waitForText(".*Create an account.*");
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "tes");
        mSolo.clickOnText("Create account");
        boolean errorMessageFound = mSolo.searchText("must contain at least 4");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountPasswordTooWeak() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("password-invalid");
        mSolo.clickOnText("Create account");
        mSolo.waitForText(".*Create an account.*");
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText("Create account");
        boolean errorMessageFound = mSolo.searchText("more secure");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountOk() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        mSolo.clickOnText("Create account");
        mSolo.waitForText(".*Create an account.*");
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText("Create account");
        assertLoggedIn();
    }
}
