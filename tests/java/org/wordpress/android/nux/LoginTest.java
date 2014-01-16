package org.wordpress.android.nux;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest.Mode;
import org.wordpress.android.ui.accounts.WelcomeActivity;
import org.xmlrpc.android.XMLRPCFactory;

public class LoginTest extends ActivityInstrumentationTestCase2<WelcomeActivity> {
    private Solo solo;

    public LoginTest() {
        super(WelcomeActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        // setUp() is run before a test case is started.
        // This is where the solo object is created.
        solo = new Solo(getInstrumentation(), getActivity());
        // reset XMLRPCFactory state
        XMLRPCFactory.factory = null;
        // reset XMLRPCFactoryTest state
        XMLRPCFactoryTest.sMode = Mode.EMPTY;
        // Init XMLRPCFactoryTest context (use instrumentation context)
        XMLRPCFactoryTest.sContext = getInstrumentation().getContext();
    }

    @Override
    public void tearDown() throws Exception {
        // tearDown() is run after a test case has finished.
        // finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        solo.finishOpenedActivities();
    }
/*
    // TODO: remove this test (we don't want to test production version of XMLRPCClient class)
    public void testWrongCredential() throws Exception {
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.clickOnText("Sign in");
        boolean errorMessageFound = solo.waitForText(".*incorrect.*");
        assertTrue("Error message not found, wrong login should fail", errorMessageFound);
    }

    public void testGoodCredentialsWithXMLRPCEmptyMock() throws Exception {
        XMLRPCFactory.factory = new XMLRPCFactoryTest();
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.clickOnText("Sign in");
        boolean errorMessageFound = solo.searchText("no network");
        assertTrue("Error message found, and that's wrong!", errorMessageFound);
    }
*/

    public void testGoodCredentialsWithXMLRPCCustomMock() throws Exception {
        XMLRPCFactoryTest.sMode = Mode.CUSTOMIZABLE;
        XMLRPCFactory.factory = new XMLRPCFactoryTest();
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.clickOnText("Sign in");
        //solo.waitForText("ekjfoizejfoij");
        boolean errorMessageFound = solo.searchText("no network");
        assertTrue("Error message found, and that's wrong!", errorMessageFound);
    }
}
