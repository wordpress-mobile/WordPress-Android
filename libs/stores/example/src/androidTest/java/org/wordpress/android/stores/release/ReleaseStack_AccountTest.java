package org.wordpress.android.stores.release;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.stores.store.AccountStore.OnAccountChanged;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_AccountTest extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    CountDownLatch mCountDownLatch;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register
        mDispatcher.register(mAccountStore);
        mDispatcher.register(this);
    }

    public void testWPCOMAuthenticationOK() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AuthenticatePayload payload = new AuthenticatePayload();
        payload.username = BuildConfig.TEST_WPCOM_USERNAME_TEST1;
        payload.password = BuildConfig.TEST_WPCOM_PASSWORD_TEST1;

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE, payload);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Get user infos
        mDispatcher.dispatch(AccountAction.FETCH);
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        assertEquals(false, event.isError);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        mCountDownLatch.countDown();
    }
}
