package org.wordpress.android;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.wordpress.android.functional.FactoryUtils;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;
import org.xmlrpc.android.ApiHelper.GenericCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ApiHelperTest extends InstrumentationTestCase {
    protected Context mTargetContext;

    public ApiHelperTest() {
        super();
        FactoryUtils.initWithTestFactories();
    }

    @Override
    protected void setUp() {
        // Clean application state
        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        TestUtils.clearApplicationState(mTargetContext);

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
    protected void tearDown() {
    }

    // This test failed before #773 was fixed
    public void testRefreshBlogContent() throws InterruptedException {
        XMLRPCFactoryTest.setPrefixAllInstances("malformed-software-version");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Blog dummyBlog = new Blog("", "", "");
        new ApiHelper.RefreshBlogContentTask(mTargetContext, dummyBlog, new GenericCallback() {
            @Override
            public void onSuccess() {
                countDownLatch.countDown();
                assertTrue(true);
            }

            @Override
            public void onFailure(ErrorType errorType, String errorMessage, Throwable throwable) {
                countDownLatch.countDown();
                assertTrue(false);
            }
        }).execute(false);
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    // This test failed before #799 was fixed
    public void testRefreshBlogContentEmptyResponse() throws InterruptedException {
        XMLRPCFactoryTest.setPrefixAllInstances("empty");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Blog dummyBlog = new Blog("", "", "");
        new ApiHelper.RefreshBlogContentTask(mTargetContext, dummyBlog, new GenericCallback() {
            @Override
            public void onSuccess() {
                countDownLatch.countDown();
                assertTrue(true);
            }

            @Override
            public void onFailure(ErrorType errorType, String errorMessage, Throwable throwable) {
                countDownLatch.countDown();
                assertTrue(false);
            }
        }).execute(false);
        countDownLatch.await(5, TimeUnit.SECONDS);
    }
}
