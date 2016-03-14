package org.wordpress.android.stores.release;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.action.SiteAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.SiteStore;
import org.wordpress.android.stores.store.SiteStore.OnSiteChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_SiteTestWPCOM extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    CountDownLatch mCountDownLatch;

    enum TEST_EVENTS {
        NONE,
        SITE_CHANGED,
        SITE_REMOVED
    }
    private TEST_EVENTS mExpectedEvent;

    private int mExpectedRowsAffected;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(mSiteStore);
        mDispatcher.register(mAccountStore);
        mDispatcher.register(this);
        // Reset expected test event
        mExpectedEvent = TEST_EVENTS.NONE;
        mExpectedRowsAffected = 0;
    }

    public void testWPCOMSiteFetchAndLogout() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload();
        payload.username = BuildConfig.TEST_WPCOM_USERNAME_TEST1;
        payload.password = BuildConfig.TEST_WPCOM_PASSWORD_TEST1;

        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE, payload);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteAction.FETCH_SITES);

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Clear WP.com sites, and wait for OnSitesRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mExpectedEvent = TEST_EVENTS.SITE_REMOVED;
        mExpectedRowsAffected = mSiteStore.getSitesCount();
        mDispatcher.dispatch(SiteAction.LOGOUT_WPCOM);

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        assertEquals(false, event.isError);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasDotComSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mExpectedEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void OnSitesRemoved(SiteStore.OnSitesRemoved event) {
        AppLog.e(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(mExpectedRowsAffected, event.mRowsAffected);
        assertEquals(false, mSiteStore.hasSite());
        assertEquals(false, mSiteStore.hasDotComSite());
        assertEquals(TEST_EVENTS.SITE_REMOVED, mExpectedEvent);
        mCountDownLatch.countDown();
    }
}
