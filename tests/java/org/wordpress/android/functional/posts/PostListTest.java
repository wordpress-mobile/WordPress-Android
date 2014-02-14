package org.wordpress.android.functional.posts;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.RenamingDelegatingContext;

import com.robotium.solo.Solo;

import org.wordpress.android.TestUtils;
import org.wordpress.android.functional.FactoryUtils;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class PostListTest extends ActivityInstrumentationTestCase2<PostsActivity> {
    private Solo mSolo;
    private Context mTargetContext;

    public PostListTest() {
        super(PostsActivity.class);
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

    public void login() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText("Sign in");
    }

    public void testCreateNewPost() throws Exception {
        login();
        mSolo.clickOnText("Posts");
        //mSolo.clickOnActionBarItem(R.id.menu_new_post);
        //mSolo.clickOnActionBarItem(R.id.menu_save_post);
        mSolo.sleep(100000);
    }
}
