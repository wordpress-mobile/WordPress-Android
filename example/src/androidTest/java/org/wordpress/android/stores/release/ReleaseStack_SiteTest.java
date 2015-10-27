package org.wordpress.android.stores.release;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.SiteAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.store.SiteStore;
import org.wordpress.android.stores.store.SiteStore.OnSiteChanged;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_SiteTest extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

    CountDownLatch mCountDownLatch;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(mSiteStore);
        mDispatcher.register(this);
    }

    public void testSelfHostedFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_1;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_1;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_1;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSelfSignedFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_3;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_3;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_3;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(10000, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.e(T.API, "count " + mSiteStore.getSitesCount());
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasSelfHostedSite());
        mCountDownLatch.countDown();
    }
}
