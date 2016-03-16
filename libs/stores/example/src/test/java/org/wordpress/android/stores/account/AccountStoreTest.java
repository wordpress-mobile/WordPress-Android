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
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.persistence.AccountSqlUtils;
import org.wordpress.android.stores.persistence.WellSqlConfig;
import org.wordpress.android.stores.store.AccountStore;

@RunWith(RobolectricTestRunner.class)
public class AccountStoreTest {
    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, AccountModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testLoadAccount() {
        AccountModel testAccount = new AccountModel();
        testAccount.setPrimaryBlogId(100);
        testAccount.setAboutMe("testAboutMe");
        AccountSqlUtils.insertOrUpdateAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockAuthenticator(), getMockAccessToken(true));
        Assert.assertEquals(testAccount, testStore.getAccount());
    }

    @Test
    public void testHasAccessToken() {
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockAuthenticator(), getMockAccessToken(true));
        Assert.assertTrue(testStore.hasAccessToken());
        testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockAuthenticator(), getMockAccessToken(false));
        Assert.assertFalse(testStore.hasAccessToken());
    }

    @Test
    public void testIsSignedIn() {
        AccountModel testAccount = new AccountModel();
        testAccount.setVisibleSiteCount(0);
        AccountSqlUtils.insertOrUpdateAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockAuthenticator(), getMockAccessToken(false));
        Assert.assertFalse(testStore.isSignedIn());
        testAccount.setVisibleSiteCount(1);
        AccountSqlUtils.insertOrUpdateAccount(testAccount);
        testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockAuthenticator(), getMockAccessToken(false));
        Assert.assertTrue(testStore.isSignedIn());
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
}
