package org.wordpress.android.stores.mocked;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.squareup.otto.Subscribe;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.persistence.WellSqlConfig;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;

import javax.inject.Inject;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 */
public class AccountStoreTest extends InstrumentationTestCase {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    public boolean mIsError;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        Context appContext = getInstrumentation().getTargetContext().getApplicationContext();

        MockedNetworkAppComponent testComponent =  DaggerMockedNetworkAppComponent.builder()
                .appContextModule(new AppContextModule(appContext))
                .build();
        testComponent.inject(this);
        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();

        // Register
        mDispatcher.register(this);
        mDispatcher.register(mAccountStore);
    }

    public void testAuthenticationOK() {
        AuthenticatePayload payload = new AuthenticatePayload();
        payload.username = "test";
        payload.password = "test";
        mIsError = false;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE, payload);
    }

    public void testAuthenticationKO() {
        AuthenticatePayload payload = new AuthenticatePayload();
        payload.username = "error";
        payload.password = "error";
        mIsError = true;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE, payload);
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        assertEquals(mIsError, event.isError);
    }
}
