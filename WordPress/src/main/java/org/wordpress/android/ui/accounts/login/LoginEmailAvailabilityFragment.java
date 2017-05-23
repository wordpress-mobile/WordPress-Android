package org.wordpress.android.ui.accounts.login;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class LoginEmailAvailabilityFragment extends Fragment {
    public enum EmailCheckState {
        IDLE,
        IN_PROGRESS,
        AVAILABLE_ON_WPCOM,
        UNAVAILABLE_ON_WPCOM,
        ERROR
    }

    public static class Events {

        public static class Clear {}

        public static class AskUpdate {}

        public static class NewCheckRequest {
            public final String emailAddress;

            public NewCheckRequest(String emailAddress) {
                this.emailAddress = emailAddress;
            }
        }

        public static class CheckUpdate {
            public final EmailCheckState emailCheckState;
            public final String emailAddress;

            public CheckUpdate(EmailCheckState emailCheckState, String emailAddress) {
                this.emailCheckState = emailCheckState;
                this.emailAddress = emailAddress;
            }
        }
    }

    private String mPendingEmail;
    private EmailCheckState mEmailCheckState = EmailCheckState.IDLE;

    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((WordPress) getActivity().getApplication()).component().inject(this);

        // retain this fragment as it serves as a headless login email check progress state keeper
        setRetainInstance(true);

        EventBus.getDefault().register(this);
        mDispatcher.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // unregister on Destroy. Will be registered earlier, on more "soft" events, like on rotate.
        mDispatcher.unregister(this);

        EventBus.getDefault().unregister(this);
    }

    public static void clear() {
        EventBus.getDefault().post(new Events.Clear());
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.Clear event) {
        mPendingEmail = null;
        mEmailCheckState = EmailCheckState.IDLE;
        emitState();
    }

    public static void askUpdate() {
        EventBus.getDefault().post(new Events.AskUpdate());
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AskUpdate event) {
        emitState();
    }

    public static void newCheckRequest(String emailAddress) {
        EventBus.getDefault().post(new Events.Clear());
        EventBus.getDefault().post(new Events.NewCheckRequest(emailAddress));
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.NewCheckRequest event) {
        if (!TextUtils.equals(mPendingEmail, event.emailAddress)) {
            // set the email pass as the pending one. Will overwrite any previous pending one if already set.
            mPendingEmail = event.emailAddress;

            // trigger the email availability check
            mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(mPendingEmail));

            // update the current state
            mEmailCheckState = EmailCheckState.IN_PROGRESS;
        }

        // emit the current state so clients can update
        emitState();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(AccountStore.OnAvailabilityChecked event) {
        if (!TextUtils.equals(mPendingEmail, event.value)) {
            // just bail. We're only interested in the last email inquired
            return;
        }

        mEmailCheckState = processEmailAvailabilityEvent(event);

        emitState();
    }

    private EmailCheckState processEmailAvailabilityEvent(AccountStore.OnAvailabilityChecked event) {
        if (event.isError()) {
            // report the error but don't bail yet.
            AppLog.e(AppLog.T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
            return EmailCheckState.ERROR;
        }

        if (event.type != AccountRestClient.IsAvailable.EMAIL) {
            AppLog.e(AppLog.T.API, "OnAvailabilityChecked type other than email! Type: " + event.error.type);
            // just bail on unexpected availability check type
            return EmailCheckState.ERROR;
        }

        return event.isAvailable ? EmailCheckState.AVAILABLE_ON_WPCOM : EmailCheckState.UNAVAILABLE_ON_WPCOM;
    }

    private void emitState() {
        EventBus.getDefault().post(new Events.CheckUpdate(mEmailCheckState, mPendingEmail));
    }
}