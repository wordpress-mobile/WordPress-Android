package org.wordpress.android.nux;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.wordpress.android.ui.accounts.WelcomeActivity;

public class LoginTest extends ActivityInstrumentationTestCase2<WelcomeActivity> {
    private Solo solo;

    public LoginTest() {
        super(WelcomeActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        //setUp() is run before a test case is started.
        //This is where the solo object is created.
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        //tearDown() is run after a test case has finished.
        //finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        solo.finishOpenedActivities();
    }

    public void testWrongCredential() throws Exception {
        solo.enterText(0, "test");
        solo.enterText(1, "test");
        solo.clickOnText("Sign in");
        boolean errorMessageFound = solo.searchText("incorrect");
        assertTrue("Error message not found, wrong login should fail", errorMessageFound);
    }
}
