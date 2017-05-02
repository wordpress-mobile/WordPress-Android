package org.wordpress.android.ui.accounts.login.nav;

import de.greenrobot.event.EventBus;

public class LoginNavigationFsm {

    public interface ContextImplementation {
        void toast(String message);
    }

    private PrologueHandler mPrologueHandler = new PrologueHandler();
    private ContextImplementation mContextImplementation;

    public LoginNavigationFsm(ContextImplementation contextImplementation) {
        mContextImplementation = contextImplementation;
    }

    public void register() {
        EventBus.getDefault().register(mPrologueHandler);
    }

    public void unregister() {
        EventBus.getDefault().unregister(mPrologueHandler);
    }

    private LoginState mCurrentLoginState = LoginState.PROLOGUE;

    public LoginState getCurrentLoginState() {
        return mCurrentLoginState;
    }

    // Implementation of LoginState.PROLOGUE
    public class PrologueHandler extends LoginStateHandler {
        public void onEventMainThread(ActionLogin event) {
            mContextImplementation.toast("Login is not implemented yet");
        }

        public void onEventMainThread(ActionSignup event) {
            mContextImplementation.toast("Signup is not implemented yet");
        }
    }
}

