package org.wordpress.android.nux;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.RenamingDelegatingContext;

import com.robotium.solo.Solo;

import org.wordpress.android.TestUtils;
import org.wordpress.android.mocks.OAuthAuthenticatorFactoryTest;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientFactory;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCFactory;

public class LoginTest extends ActivityInstrumentationTestCase2<PostsActivity> {
    private Solo solo;
    private Context targetContext;

    public LoginTest() {
        super(PostsActivity.class);
        initMocks();
    }

    public void initMocks() {
        // create test factories
        XMLRPCFactory.factory = new XMLRPCFactoryTest();
        RestClientFactory.factory = new RestClientFactoryTest();
        OAuthAuthenticatorFactory.factory = new OAuthAuthenticatorFactoryTest();
        AppLog.v(T.TESTS, "Mocks factories instantiated");
    }

    @Override
    public void setUp() throws Exception {
        // setUp() is run before a test case is started.
        // This is where the solo object is created.
        solo = new Solo(getInstrumentation(), getActivity());
        targetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");

        // Init contexts
        XMLRPCFactoryTest.sContext = getInstrumentation().getContext();
        RestClientFactoryTest.sContext = getInstrumentation().getContext();
        AppLog.v(T.TESTS, "Contexts set");

        // Set mode to Customizable
        XMLRPCFactoryTest.sMode = XMLRPCFactoryTest.Mode.CUSTOMIZABLE;
        RestClientFactoryTest.sMode = RestClientFactoryTest.Mode.CUSTOMIZABLE;
        AppLog.v(T.TESTS, "Modes set to customizable");

        // Clean application state
        TestUtils.clearDefaultSharedPreferences(targetContext);
        TestUtils.dropDB(targetContext);
    }

    @Override
    public void tearDown() throws Exception {
        // tearDown() is run after a test case has finished.
        // finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        solo.finishOpenedActivities();
    }

    public void assertMenuDrawerIsOpen() {
        boolean drawerOpen = solo.searchText("Reader");
        drawerOpen &= solo.searchText("Posts");
        assertTrue("Menu drawer seems closed", drawerOpen);
    }

    public void testGoodCredentials() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.clickOnText("Sign in");
        assertMenuDrawerIsOpen();
    }

    public void testBadCredentials() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("login-failure");
        XMLRPCFactoryTest.setPrefixAllInstances("login-failure");
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.clickOnText("Sign in");
        boolean errorMessageFound = solo.searchText("username or password");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountInvalidEmail() throws Exception {
        solo.clickOnText("Create account");
        solo.waitForText(".*Create an account.*");
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.enterText(2, "test");
        solo.clickOnText("Create account");
        boolean errorMessageFound = solo.searchText("address is not valid");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountUsernameExists() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("username-exists");
        solo.clickOnText("Create account");
        solo.waitForText(".*Create an account.*");
        solo.enterText(0, "test@test.com");
        solo.enterText(1, "test");
        solo.enterText(2, "test");
        solo.clickOnText("Create account");
        boolean errorMessageFound = solo.searchText("username already exists");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountPasswordTooShort() throws Exception {
        solo.clickOnText("Create account");
        solo.waitForText(".*Create an account.*");
        solo.enterText(0, "test@test.com");
        solo.enterText(1, "test");
        solo.enterText(2, "tes");
        solo.clickOnText("Create account");
        boolean errorMessageFound = solo.searchText("must contain at least 4");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountPasswordTooWeak() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("password-invalid");
        solo.clickOnText("Create account");
        solo.waitForText(".*Create an account.*");
        solo.enterText(0, "test@test.com");
        solo.enterText(1, "test");
        solo.enterText(2, "test");
        solo.clickOnText("Create account");
        boolean errorMessageFound = solo.searchText("more secure");
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountOk() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        solo.clickOnText("Create account");
        solo.waitForText(".*Create an account.*");
        solo.enterText(0, "test@test.com");
        solo.enterText(1, "test");
        solo.enterText(2, "test");
        solo.clickOnText("Create account");
        assertMenuDrawerIsOpen();
    }
}
