package org.wordpress.android.nux;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.wordpress.android.mocks.OAuthAuthenticatorFactoryTest;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientFactory;
import org.wordpress.android.ui.accounts.WelcomeActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCFactory;

public class LoginTest extends ActivityInstrumentationTestCase2<WelcomeActivity> {
    private Solo solo;

    public LoginTest() {
        super(WelcomeActivity.class);
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
        solo.finishOpenedActivities();
    }

    public void testGoodCredentials() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.clickOnText("Sign in");
        solo.waitForActivity(".*ost.*");
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
}
