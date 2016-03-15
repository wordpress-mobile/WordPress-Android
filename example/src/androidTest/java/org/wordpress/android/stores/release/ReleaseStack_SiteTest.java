package org.wordpress.android.stores.release;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.SiteAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.stores.model.SiteModel;
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
    @Inject AccountStore mAccountStore;
    @Inject HTTPAuthManager mHTTPAuthManager;

    CountDownLatch mCountDownLatch;

    enum TEST_EVENTS {
        NONE,
        HTTP_AUTH_ERROR,
        SITE_CHANGED,
        SITE_REMOVED
    }
    private TEST_EVENTS mExpectedEvent;

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
    }

    public void testSelfHostedSimpleFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_SIMPLE;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleContributorFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_CONTRIB;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_CONTRIB;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_SIMPLE_CONTRIB;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedMutliSiteFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_MULTISITE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_MULTISITE;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_MULTISITE;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleHTTPSFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_VALID_SSL;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSelfSignedSSLFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // TODO: should get a SSL Warning event

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedHTTPAuthFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH;

        // We're expecting a HTTP_AUTH_ERROR
        mExpectedEvent = TEST_EVENTS.HTTP_AUTH_ERROR;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH, BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH, null);
        // Retry to fetch sites, this time we expect a site refresh
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // Wait for a network response
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedHTTPAuthFetchSites2() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH, BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH, null);

        mCountDownLatch = new CountDownLatch(1);
        // Retry to fetch sites,we expect a site refresh
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        // Wait for a network response
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedFetchAndDeleteSite() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;
        payload.xmlrpcEndpoint = BuildConfig.TEST_WPORG_URL_SH_SIMPLE;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mExpectedEvent = TEST_EVENTS.SITE_REMOVED;
        mCountDownLatch = new CountDownLatch(1);
        SiteModel dotOrgSite = mSiteStore.getDotOrgSites().get(0);
        mDispatcher.dispatch(SiteAction.REMOVE_SITE, dotOrgSite);

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasDotOrgSite());
        assertEquals(mExpectedEvent, TEST_EVENTS.SITE_CHANGED);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void OnSitesRemoved(SiteStore.OnSitesRemoved event) {
        AppLog.e(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(false, mSiteStore.hasSite());
        assertEquals(false, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_REMOVED, mExpectedEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError) {
            AppLog.i(T.TESTS, "error " + event.authError);
            if (event.authError == AuthError.HTTP_AUTH_ERROR) {
                assertEquals(mExpectedEvent, TEST_EVENTS.HTTP_AUTH_ERROR);
                mCountDownLatch.countDown();
            }
        }
    }
}
