package org.wordpress.android;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.ActivityInstrumentationTestCase2;
import android.test.RenamingDelegatingContext;

import com.robotium.solo.Solo;

import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.AppLog;

public class ActivityRobotiumTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    protected Solo mSolo;
    protected Context mTargetContext;

    public ActivityRobotiumTestCase(Class<T> activityClass) {
        super(activityClass);
        FactoryUtils.initWithTestFactories();
    }

    @Override
    public void setUp() throws Exception {
        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        TestUtils.clearApplicationState(mTargetContext);

        // setUp() is run before a test case is started.
        // This is where the mSolo object is created.
        mSolo = new Solo(getInstrumentation(), getActivity());
        forceLogInScreen();

        // Init contexts
        XMLRPCFactoryTest.sContext = getInstrumentation().getContext();
        RestClientFactoryTest.sContext = getInstrumentation().getContext();
        AppLog.v(AppLog.T.TESTS, "Contexts set");

        // Set mode to Customizable
        XMLRPCFactoryTest.sMode = XMLRPCFactoryTest.Mode.CUSTOMIZABLE_XML;
        RestClientFactoryTest.sMode = RestClientFactoryTest.Mode.CUSTOMIZABLE;
        AppLog.v(AppLog.T.TESTS, "Modes set to customizable");

        // Set default variant
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");

        SQLiteDatabase db = TestUtils.loadDBFromDump(mTargetContext, getInstrumentation().getContext(),
                "empty_tables.sql");
    }

    @Override
    public void tearDown() throws Exception {
        // tearDown() is run after a test case has finished.
        // finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        mSolo.finishOpenedActivities();
        getActivity().finish();
        super.tearDown();
    }

    public void login() throws Exception {
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText(mSolo.getString(R.string.sign_in));
    }

    protected void forceLogInScreen() throws Exception {
        boolean isLoginScreenVisible = mSolo.searchText(mSolo.getString(R.string.username)) && mSolo.searchText(
                mSolo.getString(R.string.password));
        if (!isLoginScreenVisible && getActivity() instanceof WPActionBarActivity) {
            WPActionBarActivity wpActionBarActivity = (WPActionBarActivity) getActivity();
            TestUtils.clearApplicationState(mTargetContext);
            wpActionBarActivity.setupCurrentBlog();
        }
    }
}
