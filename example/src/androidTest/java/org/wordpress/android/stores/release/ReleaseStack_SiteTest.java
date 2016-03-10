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

    public void testSelfHostedSimpleFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_SIMPLE;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleContributorFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_CONTRIB;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_CONTRIB;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_SIMPLE_CONTRIB;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(10000, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedMutliSiteFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_MULTISITE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_MULTISITE;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_MULTISITE;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(10000, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleHTTPSFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_VALID_SSL;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(10000, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSelfSignedSSLFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // TODO: should get a SSL Warning event

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(10000, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedHTTPAuthFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // TODO: should get a HTTP AUTH required event
        // then reply with TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH and TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH
        // to finish the setup

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
