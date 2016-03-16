package org.wordpress.android.stores.release;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.PostAccountSettingsPayload;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.stores.store.AccountStore.OnAccountChanged;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;

import java.util.HashMap;
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

    enum ACCOUNT_TEST_ACTIONS {
        NONE,
        AUTHENTICATE,
        AUTHENTICATE_ERROR,
        FETCHED,
        POSTED,
    }

    ACCOUNT_TEST_ACTIONS mExpectedAction;
    String mExpectedValue;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register
        mDispatcher.register(mAccountStore);
        mDispatcher.register(this);
        mExpectedAction = ACCOUNT_TEST_ACTIONS.NONE;
    }

    public void testWPCOMAuthenticationOK() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
    }

    public void testWPCOMAuthenticationError() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE_ERROR;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_BAD_PASSWORD);
    }

    public void testWPCOMFetch() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }
        mExpectedAction = ACCOUNT_TEST_ACTIONS.FETCHED;
        mDispatcher.dispatch(AccountAction.FETCH);
        mCountDownLatch = new CountDownLatch(2);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWPCOMPost() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }
        mExpectedAction = ACCOUNT_TEST_ACTIONS.POSTED;
        PostAccountSettingsPayload payload = new PostAccountSettingsPayload();
        mExpectedValue = String.valueOf(System.currentTimeMillis());
        payload.params = new HashMap<>();
        payload.params.put("description", mExpectedValue);
        mDispatcher.dispatch(AccountAction.POST_SETTINGS, payload);
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError) {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.AUTHENTICATE_ERROR);
        } else {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.AUTHENTICATE);
        }
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.FETCHED);
            assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.FETCHED);
            assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.POST_SETTINGS) {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.POSTED);
            assertEquals(mExpectedValue, mAccountStore.getAccount().getAboutMe());
        }
        mCountDownLatch.countDown();
    }

    private void authenticate(String username, String password) throws InterruptedException {
        AuthenticatePayload payload = new AuthenticatePayload();
        payload.username = username;
        payload.password = password;

        mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE, payload);
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
