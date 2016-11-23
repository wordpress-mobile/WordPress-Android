package org.wordpress.android;

import android.content.Context;
import android.content.Intent;
import android.test.RenamingDelegatingContext;
import android.test.ServiceTestCase;

import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.AppLog;

public class PostUploadServiceTest extends ServiceTestCase<PostUploadService> {
    protected Context testContext;
    protected Context targetContext;

    public PostUploadServiceTest() {
        super(PostUploadService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        FactoryUtils.initWithTestFactories();

        String namespace = BuildConfig.FLAVOR.equals("wasabi") ? "org.wordpress.android.beta"
                : "org.wordpress.android";
        testContext = getContext().createPackageContext(namespace, Context.CONTEXT_IGNORE_SECURITY);
        targetContext = new RenamingDelegatingContext(getContext(), "test_");

        // Init contexts
        XMLRPCFactoryTest.sContext = getContext();
        RestClientFactoryTest.sContext = getContext();
        AppLog.v(AppLog.T.TESTS, "Contexts set");

        // Set mode to Customizable
        XMLRPCFactoryTest.sMode = XMLRPCFactoryTest.Mode.CUSTOMIZABLE_XML;
        RestClientFactoryTest.sMode = RestClientFactoryTest.Mode.CUSTOMIZABLE;
        AppLog.v(AppLog.T.TESTS, "Modes set to customizable");
    }

    @Override
    protected void tearDown() throws Exception {
        FactoryUtils.clearFactories();
        super.tearDown();
    }

    public void testStartable() {
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), PostUploadService.class);
        startService(startIntent);
    }

    // test reproducing https://github.com/wordpress-mobile/WordPress-Android/issues/884
    // Following test is valid but won't be fixed now (it will with the post editor rewrite)
    /*
    public void testUploadMalformedPostNullPostId() throws Exception {
        // init a test db containing a few blogs and posts
        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext, "taliwutt-blogs-sample.sql");
        WordPressDB wpdb = WordPress.wpDB;

        // callback should be called 3 times
        final CountDownLatch countDownLatch = new CountDownLatch(3);

        // trick to have a mutable final int
        final int[] notifyCount = {0};
        final int[] cancelCount = {0};
        SystemServiceFactoryTest.sNotificationCallback = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if ("notify".equals(invocation.getMethod().getName())) {
                    notifyCount[0] += 1;
                }
                if ("cancel".equals(invocation.getMethod().getName())) {
                    cancelCount[0] += 1;
                }
                countDownLatch.countDown();
                return null;
            }
        };

        // get an existing uploaded post (defined in the previously loaded db dump)
        int postId = 27;
        Post post = wpdb.getPostForLocalTablePostId(postId);

        // fake the remote post id to null
        post.setRemotePostId(null);

        // push it to the PostUploadService
        PostUploadService.addPostToUpload(post);
        startService(new Intent(getContext(), PostUploadService.class));

        // wait for the response
        countDownLatch.await(15, TimeUnit.SECONDS);
        assertTrue("NotificationManager.cancel must be called at least once - see #884",
                cancelCount[0] == 1 && notifyCount[0] == 2);
    }
    */
}
