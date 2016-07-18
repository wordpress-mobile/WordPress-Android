package org.wordpress.android.stores.account;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.persistence.AccountSqlUtils;
import org.wordpress.android.stores.persistence.WellSqlConfig;
import org.wordpress.android.stores.store.AccountStore;

import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
public class AccountStoreTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(mContext, AccountModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testLoadAccount() {
        AccountModel testAccount = new AccountModel();
        testAccount.setPrimaryBlogId(100);
        testAccount.setAboutMe("testAboutMe");
        AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), getMockAccessToken(true));
        Assert.assertEquals(testAccount, testStore.getAccount());
    }

    @Test
    public void testHasAccessToken() {
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), getMockAccessToken(true));
        Assert.assertTrue(testStore.hasAccessToken());
        testStore = new AccountStore(new Dispatcher(), getMockRestClient(), getMockSelfHostedEndpointFinder(),
                getMockAuthenticator(), getMockAccessToken(false));
        Assert.assertFalse(testStore.hasAccessToken());
    }

    @Test
    public void testIsSignedIn() {
        AccountModel testAccount = new AccountModel();
        testAccount.setVisibleSiteCount(0);
        AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), getMockAccessToken(false));
        Assert.assertFalse(testStore.hasAccessToken());
        testAccount.setVisibleSiteCount(1);
        AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount);
        testStore = new AccountStore(new Dispatcher(), getMockRestClient(), getMockSelfHostedEndpointFinder(),
                getMockAuthenticator(), getMockAccessToken(true));
        Assert.assertTrue(testStore.hasAccessToken());
    }

    @Test
    public void testSignOut() throws Exception {
        AccountModel testAccount = new AccountModel();
        AccessToken testToken = new AccessToken(mContext);
        testToken.set("TESTTOKEN");
        testAccount.setUserId(24);
        AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), testToken);
        Assert.assertTrue(testStore.hasAccessToken());
        // Signout is private (and it should remain private)
        Method privateMethod = AccountStore.class.getDeclaredMethod("signOut");
        privateMethod.setAccessible(true);
        privateMethod.invoke(testStore);
        Assert.assertFalse(testStore.hasAccessToken());
        Assert.assertNull(AccountSqlUtils.getAccountByLocalId(testAccount.getId()));
    }

    private AccountRestClient getMockRestClient() {
        return Mockito.mock(AccountRestClient.class);
    }

    private Authenticator getMockAuthenticator() {
        return Mockito.mock(Authenticator.class);
    }

    private AccessToken getMockAccessToken(boolean exists) {
        AccessToken mock = Mockito.mock(AccessToken.class);
        Mockito.when(mock.exists()).thenReturn(exists);
        return mock;
    }

    private SelfHostedEndpointFinder getMockSelfHostedEndpointFinder() {
        return Mockito.mock(SelfHostedEndpointFinder.class);
    }
}
